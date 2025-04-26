package org.example;

import org.example.Block;
import org.joml.*;
import org.joml.Math;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.Objects;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class VoxelEngine {
    private long window;
    private Camera camera;
    private Chunk chunk;
    private Shader shader;
    private double lastX = 400, lastY = 300;
    private boolean firstMouse = true;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Failed to initialize GLFW");

        glfwDefaultWindowHints();
        window = glfwCreateWindow(800, 600, "Voxel Engine", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create GLFW window");

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);

        camera = new Camera();
        chunk = new Chunk();
        chunk.generateFlatTerrain();
        chunk.init();

        Texture.loadTextures();
        shader = new Shader(loadShaderFile("vertex.glsl"), loadShaderFile("fragment.glsl"));

        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (firstMouse) {
                lastX = xpos;
                lastY = ypos;
                firstMouse = false;
            }

            double dx = xpos - lastX;
            double dy = lastY - ypos;
            lastX = xpos;
            lastY = ypos;

            camera.processMouse(dx, dy);
        });

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            processInput();

            glClearColor(0.5f, 0.7f, 1.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            camera.update();

            shader.use();
            shader.setMat4("projection", camera.getProjectionMatrix());
            shader.setMat4("view", camera.getViewMatrix());

            chunk.render(shader);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void processInput() {
        camera.processKeyboard(window);

        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS)
            glfwSetWindowShouldClose(window, true);

        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS)
            handleBlockBreaking();

        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS)
            handleBlockPlacement();
    }

    private void handleBlockBreaking() {
        RaycastResult result = raycast();
        if (result != null && isValidBlockPosition(result.blockPos)) {
            chunk.blocks[result.blockPos.x][result.blockPos.y][result.blockPos.z] = Block.AIR;
        }
    }

    private void handleBlockPlacement() {
        RaycastResult result = raycast();
        if (result != null) {
            Vector3i placePos = new Vector3i(result.blockPos)
                    .add((int) Math.signum(result.faceNormal.x),
                            (int) Math.signum(result.faceNormal.y),
                            (int) Math.signum(result.faceNormal.z));

            if (isValidBlockPosition(placePos)) {
                chunk.blocks[placePos.x][placePos.y][placePos.z] = Block.GRASS;
            }
        }
    }

    private boolean isValidBlockPosition(Vector3i pos) {
        return pos.x >= 0 && pos.x < Chunk.CHUNK_SIZE &&
                pos.y >= 0 && pos.y < Chunk.CHUNK_SIZE &&
                pos.z >= 0 && pos.z < Chunk.CHUNK_SIZE;
    }

    private String loadShaderFile(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get("src/main/resources/shaders/" + path)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader: " + path, e);
        }
    }

    private RaycastResult raycast() {
        // Get mouse coordinates
        DoubleBuffer xBuf = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer yBuf = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(window, xBuf, yBuf);
        double mouseX = xBuf.get(0);
        double mouseY = yBuf.get(0);

        // Convert to NDC
        float x = (float) ((2.0 * mouseX) / 800 - 1.0f);
        float y = (float) (1.0 - (2.0 * mouseY) / 600);
        Vector4f rayClip = new Vector4f(x, y, -1.0f, 1.0f);

        // Convert to world space
        Matrix4f invProjection = new Matrix4f(camera.getProjectionMatrix()).invert();
        Vector4f rayEye = invProjection.transform(rayClip);
        rayEye.z = -1.0f;
        rayEye.w = 0.0f;

        Matrix4f invView = new Matrix4f(camera.getViewMatrix()).invert();
        Vector4f rayWorld = invView.transform(rayEye);
        Vector3f rayDir = new Vector3f(rayWorld.x, rayWorld.y, rayWorld.z).normalize();

        // Ray marching
        Vector3f currentPos = new Vector3f(camera.position);
        Vector3f step = rayDir.mul(0.05f);
        Vector3i lastValid = null;
        Vector3f faceNormal = new Vector3f();

        for (int i = 0; i < 100; i++) {
            Vector3i blockPos = new Vector3i(
                    (int) Math.floor(currentPos.x),
                    (int) Math.floor(currentPos.y),
                    (int) Math.floor(currentPos.z)
            );

            if (isValidBlockPosition(blockPos)) {
                if (chunk.blocks[blockPos.x][blockPos.y][blockPos.z] != Block.AIR) {
                    // Calculate face normal
                    faceNormal.set(
                            currentPos.x - blockPos.x - 0.5f,
                            currentPos.y - blockPos.y - 0.5f,
                            currentPos.z - blockPos.z - 0.5f
                    );

                    Vector3f absNormal = new Vector3f(faceNormal).absolute();
                    int dominantAxis = absNormal.maxComponent(); // Returns 0 (x), 1 (y), or 2 (z)

                    faceNormal.set(0, 0, 0);
                    switch (dominantAxis) {
                        case 0 -> faceNormal.x = Math.signum(faceNormal.x);
                        case 1 -> faceNormal.y = Math.signum(faceNormal.y);
                        case 2 -> faceNormal.z = Math.signum(faceNormal.z);
                    }

                    return new RaycastResult(blockPos, faceNormal);
                }
                lastValid = blockPos;
            }
            currentPos.add(step);
        }

        // If no block hit, return last valid position in chunk
        return lastValid != null ? new RaycastResult(lastValid, new Vector3f()) : null;
    }


    private void cleanup() {
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        Objects.requireNonNull(glfwSetErrorCallback(null)).free();
    }

    public static void main(String[] args) {
        new VoxelEngine().run();
    }

    private static class RaycastResult {
        public final Vector3i blockPos;
        public final Vector3f faceNormal;

        public RaycastResult(Vector3i blockPos, Vector3f faceNormal) {
            this.blockPos = blockPos;
            this.faceNormal = faceNormal;
        }
    }
}
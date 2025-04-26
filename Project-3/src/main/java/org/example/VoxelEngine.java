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
    private Shader crosshairShader;
    private int crosshairVAO;

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
        glEnable(GL_POINT_SMOOTH);
        glHint(GL_POINT_SMOOTH_HINT, GL_NICEST);

        camera = new Camera();
        chunk = new Chunk();
        chunk.generateFlatTerrain();
        chunk.init();

        Texture.loadTextures();
        shader = new Shader(loadShaderFile("vertex.glsl"), loadShaderFile("fragment.glsl"));
        setupCrosshair();

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
        long lastFrameTime = System.nanoTime();

        while (!glfwWindowShouldClose(window)) {
            long currentTime = System.nanoTime();
            float deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f;
            lastFrameTime = currentTime;

            Vector3f origin = new Vector3f(camera.position);
            Vector3f direction = new Vector3f(camera.front).normalize();

            Raycast.HitResult result = Raycast.castRay(origin, direction, chunk, 10.0f);

            if (result != null) {
                if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
                    // Destroy block
                    Vector3i pos = result.blockPos;
                    if (Raycast.inChunkBounds(pos)) {
                        chunk.blocks[pos.x][pos.y][pos.z] = Block.AIR;
                    }
                } else if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
                    // Place block on the face hit
                    Vector3i placePos = new Vector3i(result.blockPos).add(result.hitNormal);

                    if (Raycast.inChunkBounds(placePos)
                            && chunk.blocks[placePos.x][placePos.y][placePos.z] == Block.AIR) {
                        chunk.blocks[placePos.x][placePos.y][placePos.z] = Block.GRASS;
                    }
                }
            }

            if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, true);
            }

            camera.processKeyboard(window, deltaTime);

            glClearColor(0.5f, 0.7f, 1.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            camera.update();

            shader.use();
            shader.setMat4("projection", camera.getProjectionMatrix());
            shader.setMat4("view", camera.getViewMatrix());
            chunk.render(shader);

            renderCrosshair();

            glfwSwapBuffers(window);
            glfwPollEvents();
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

    private void cleanup() {
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        Objects.requireNonNull(glfwSetErrorCallback(null)).free();
    }

    private void setupCrosshair() {
        crosshairShader = new Shader(
                loadShaderFile("crosshair.vert"),
                loadShaderFile("crosshair.frag")
        );

        crosshairVAO = glGenVertexArrays();
        glBindVertexArray(crosshairVAO);
        glBindVertexArray(0);
    }

    private void renderCrosshair() {
        // Disable depth test
        glDisable(GL_DEPTH_TEST);

        // Use crosshair shader
        crosshairShader.use();

        // Draw
        glBindVertexArray(crosshairVAO);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);

        // Restore state
        glEnable(GL_DEPTH_TEST);
    }



    public static void main(String[] args) {
        new VoxelEngine().run();
    }
}
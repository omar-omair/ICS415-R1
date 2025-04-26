package org.example;

import org.joml.*;
import org.joml.Math;

import static org.lwjgl.glfw.GLFW.*;

public class Camera {
    public final Vector3f position = new Vector3f(0, 10, 0);
    public final Vector2f rotation = new Vector2f();
    public final Matrix4f viewMatrix = new Matrix4f();
    public final Matrix4f projectionMatrix = new Matrix4f();
    public final Vector3f front = new Vector3f(0, 0, -1);
    public final Vector3f up = new Vector3f(0, 1, 0);
    public final Vector3f right = new Vector3f();
    public float speed = 0.1f;
    public float sensitivity = 0.1f;

    public Camera() {
        projectionMatrix.setPerspective((float) Math.toRadians(70), 800f/600f, 0.1f, 1000f);
        updateVectors();
    }

    public void update() {
        updateVectors();
        viewMatrix.identity()
                .lookAt(position, position.add(front, new Vector3f()), up);
    }

    public void processKeyboard(long window) {
        Vector3f movement = new Vector3f();

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
            movement.add(front);
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
            movement.sub(front);
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
            movement.sub(right);
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
            movement.add(right);

        if (movement.lengthSquared() > 0)
            position.add(movement.normalize().mul(speed));
    }

    public void processMouse(double dx, double dy) {
        rotation.x += (float) dy * sensitivity;
        rotation.y += (float) dx * sensitivity;
        rotation.x = Math.clamp(rotation.x, -89, 89);
    }

    private void updateVectors() {
        front.x = (float) (Math.cos(Math.toRadians(rotation.y)) * Math.cos(Math.toRadians(rotation.x)));
        front.y = (float) Math.sin(Math.toRadians(rotation.x));
        front.z = (float) (Math.sin(Math.toRadians(rotation.y)) * Math.cos(Math.toRadians(rotation.x)));
        front.normalize();

        right.set(front).cross(up).normalize();
    }

    public Matrix4f getViewMatrix() { return viewMatrix; }
    public Matrix4f getProjectionMatrix() { return projectionMatrix; }
}
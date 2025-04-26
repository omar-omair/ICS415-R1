package org.example;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;

public class Shader {
    private final int programID;

    public Shader(String vertexCode, String fragmentCode) {
        int vertexID = compileShader(vertexCode, GL_VERTEX_SHADER);
        int fragmentID = compileShader(fragmentCode, GL_FRAGMENT_SHADER);

        programID = glCreateProgram();
        glAttachShader(programID, vertexID);
        glAttachShader(programID, fragmentID);
        glLinkProgram(programID);

        checkCompileErrors();
        glDeleteShader(vertexID);
        glDeleteShader(fragmentID);
    }

    public void use() {
        glUseProgram(programID);
    }

    public void setMat4(String name, Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            matrix.get(fb);
            glUniformMatrix4fv(glGetUniformLocation(programID, name), false, fb);
        }
    }

    private int compileShader(String code, int type) {
        int shader = glCreateShader(type);
        glShaderSource(shader, code);
        glCompileShader(shader);
        checkCompileErrors(shader);
        return shader;
    }

    private void checkCompileErrors(int shader) {
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader compilation failed:\n" + glGetShaderInfoLog(shader));
        }
    }

    private void checkCompileErrors() {
        if (glGetProgrami(programID, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader linking failed:\n" + glGetProgramInfoLog(programID));
        }
    }
}
package com.shooter;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    private long window;
    private int width = 800;
    private int height = 600;
    
    // Camera variables
    private float cameraX = 0.0f;
    private float cameraY = 0.0f;
    private float cameraZ = 5.0f;
    private float yaw = -90.0f;
    private float pitch = 0.0f;
    private boolean firstMouse = true;
    private float lastX = 400;
    private float lastY = 300;
    
    // Movement
    private boolean[] keys = new boolean[1024];
    private float deltaTime = 0.0f;
    private float lastFrame = 0.0f;
    
    // Targets
    private float[] targetX = {2.0f, -2.0f, 0.0f, 3.0f, -3.0f};
    private float[] targetY = {0.0f, 1.0f, 0.5f, -1.0f, 1.5f};
    private float[] targetZ = {-3.0f, -3.0f, -4.0f, -5.0f, -4.0f};
    private boolean[] targetActive = {true, true, true, true, true};
    private int score = 0;
    
    // Shooting
    private boolean canShoot = true;
    private float shootCooldown = 0.3f;
    private float shootTimer = 0.0f;

    public void run() {
        init();
        loop();
        
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        
        window = glfwCreateWindow(width, height, "Simple FPS Shooter - Score: " + score, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (key < keys.length) {
                if (action == GLFW_PRESS) {
                    keys[key] = true;
                } else if (action == GLFW_RELEASE) {
                    keys[key] = false;
                }
            }
            
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });
        
        glfwSetCursorPosCallback(window, (w, xpos, ypos) -> {
            float x = (float) xpos;
            float y = (float) ypos;
            
            if (firstMouse) {
                lastX = x;
                lastY = y;
                firstMouse = false;
            }
            
            float xoffset = x - lastX;
            float yoffset = lastY - y;
            lastX = x;
            lastY = y;
            
            float sensitivity = 0.1f;
            xoffset *= sensitivity;
            yoffset *= sensitivity;
            
            yaw += xoffset;
            pitch += yoffset;
            
            if (pitch > 89.0f) pitch = 89.0f;
            if (pitch < -89.0f) pitch = -89.0f;
        });
        
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        
        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS && canShoot) {
                checkHit();
                shootTimer = 0.0f;
                canShoot = false;
            }
        });
        
        String vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor()).toString();
        glfwSetWindowPos(window, 100, 100);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        
        GL.createCapabilities();
        glClearColor(0.5f, 0.7f, 0.9f, 1.0f);
        glEnable(GL_DEPTH_TEST);
    }

    private void checkHit() {
        float camDirX = (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        float camDirY = (float) Math.sin(Math.toRadians(pitch));
        float camDirZ = (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        
        for (int i = 0; i < targetActive.length; i++) {
            if (!targetActive[i]) continue;
            
            float dx = targetX[i] - cameraX;
            float dy = targetY[i] - cameraY;
            float dz = targetZ[i] - cameraZ;
            
            float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
            
            float dot = (camDirX * dx + camDirY * dy + camDirZ * dz) / dist;
            
            if (dot > 0.95f && dist < 10.0f) {
                targetActive[i] = false;
                score += 100;
                glfwSetWindowTitle(window, "Simple FPS Shooter - Score: " + score);
                
                // Respawn target after 2 seconds
                final int index = i;
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        targetActive[index] = true;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                break;
            }
        }
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            float currentFrame = (float) glfwGetTime();
            deltaTime = currentFrame - lastFrame;
            lastFrame = currentFrame;
            
            if (shootTimer < shootCooldown) {
                shootTimer += deltaTime;
            } else {
                canShoot = true;
            }
            
            processInput();
            
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            render();
            
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void processInput() {
        float speed = 5.0f * deltaTime;
        
        float cosYaw = (float) Math.cos(Math.toRadians(yaw));
        float sinYaw = (float) Math.sin(Math.toRadians(yaw));
        
        if (keys[GLFW_KEY_W]) {
            cameraX += sinYaw * speed;
            cameraZ -= cosYaw * speed;
        }
        if (keys[GLFW_KEY_S]) {
            cameraX -= sinYaw * speed;
            cameraZ += cosYaw * speed;
        }
        if (keys[GLFW_KEY_A]) {
            cameraX -= cosYaw * speed;
            cameraZ -= sinYaw * speed;
        }
        if (keys[GLFW_KEY_D]) {
            cameraX += cosYaw * speed;
            cameraZ += sinYaw * speed;
        }
        if (keys[GLFW_KEY_SPACE]) {
            cameraY += speed;
        }
        if (keys[GLFW_KEY_LEFT_SHIFT]) {
            cameraY -= speed;
        }
    }

    private void render() {
        // Draw floor
        glBegin(GL_QUADS);
        glColor3f(0.3f, 0.3f, 0.3f);
        glVertex3f(-10.0f, -2.0f, -10.0f);
        glVertex3f(10.0f, -2.0f, -10.0f);
        glVertex3f(10.0f, -2.0f, 10.0f);
        glVertex3f(-10.0f, -2.0f, 10.0f);
        glEnd();
        
        // Draw walls
        glBegin(GL_QUADS);
        glColor3f(0.5f, 0.4f, 0.3f);
        // Back wall
        glVertex3f(-10.0f, -2.0f, -10.0f);
        glVertex3f(10.0f, -2.0f, -10.0f);
        glVertex3f(10.0f, 5.0f, -10.0f);
        glVertex3f(-10.0f, 5.0f, -10.0f);
        glEnd();
        
        // Draw targets
        for (int i = 0; i < targetActive.length; i++) {
            if (targetActive[i]) {
                drawTarget(targetX[i], targetY[i], targetZ[i]);
            }
        }
        
        // Draw crosshair
        drawCrosshair();
    }

    private void drawTarget(float x, float y, float z) {
        float size = 0.5f;
        
        // Red circle (target)
        glBegin(GL_TRIANGLE_FAN);
        glColor3f(1.0f, 0.0f, 0.0f);
        glVertex3f(x, y, z);
        for (int i = 0; i <= 20; i++) {
            double angle = 2.0 * Math.PI * i / 20.0;
            glVertex3f(
                x + (float) Math.cos(angle) * size,
                y + (float) Math.sin(angle) * size,
                z
            );
        }
        glEnd();
        
        // White center
        glBegin(GL_TRIANGLE_FAN);
        glColor3f(1.0f, 1.0f, 1.0f);
        glVertex3f(x, y, z);
        for (int i = 0; i <= 20; i++) {
            double angle = 2.0 * Math.PI * i / 20.0;
            glVertex3f(
                x + (float) Math.cos(angle) * size * 0.3f,
                y + (float) Math.sin(angle) * size * 0.3f,
                z
            );
        }
        glEnd();
    }

    private void drawCrosshair() {
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);
        
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        
        glDisable(GL_DEPTH_TEST);
        
        glBegin(GL_LINES);
        glColor3f(0.0f, 1.0f, 0.0f);
        // Horizontal line
        glVertex2f(width/2 - 10, height/2);
        glVertex2f(width/2 + 10, height/2);
        // Vertical line
        glVertex2f(width/2, height/2 - 10);
        glVertex2f(width/2, height/2 + 10);
        glEnd();
        
        glEnable(GL_DEPTH_TEST);
        
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }

    public static void main(String[] args) {
        new Main().run();
    }
}

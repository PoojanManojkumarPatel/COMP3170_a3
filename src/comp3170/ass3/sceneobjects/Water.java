package comp3170.ass3.sceneobjects;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import comp3170.GLBuffers;
import comp3170.OpenGLException;
import comp3170.SceneObject;
import comp3170.Shader;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class Water extends SceneObject {
    private Shader shader;
    private Camera cam;
    private int positionBuffer;
    private int normalBuffer;
    private int uvBuffer;
    private int indexBuffer;
    private int indexCount;
    private static final float WATER_HEIGHT = 22.2f;
    private static final float TERRAIN_SIZE = 100f;

    public Water(Camera cam) {
        this.cam = cam;
        int gridSize = 200;  // Grid resolution
        float size = 100f;   // Total size of water plane
        
        List<Vector3f> positions = new ArrayList<>();
        List<Vector2f> uvs = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

     // Generate grid vertices
        for (int i = 0; i <= gridSize; i++) {
            for (int j = 0; j <= gridSize; j++) {
                // Calculate position to match heightmap coordinates
                float x = (j / (float)gridSize) * TERRAIN_SIZE;  // 0 to 100
                float z = (i / (float)gridSize) * TERRAIN_SIZE;  // 0 to 100
                
                // Position vertex at WATER_HEIGHT
                positions.add(new Vector3f(x, WATER_HEIGHT, z));
                
                // Calculate UVs
                float u = j / (float)gridSize * 10f;  // Tile texture 10 times
                float v = i / (float)gridSize * 10f;
                uvs.add(new Vector2f(u, v));
                
                // Add upward-facing normal
                normals.add(new Vector3f(0, 1, 0));
            }
        }

        // Generate indices for triangles
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                int row1 = i * (gridSize + 1);
                int row2 = (i + 1) * (gridSize + 1);
                
                // First triangle
                indices.add(row1 + j);
                indices.add(row2 + j);
                indices.add(row1 + j + 1);
                
                // Second triangle
                indices.add(row1 + j + 1);
                indices.add(row2 + j);
                indices.add(row2 + j + 1);

                // Calculate normals for these vertices
                calculateNormals(positions, normals, row1 + j, row2 + j, row1 + j + 1);
                calculateNormals(positions, normals, row1 + j + 1, row2 + j, row2 + j + 1);
            }
        }
        
     // Load shader
        try {
            shader = new Shader(
                new File("src/comp3170/ass3/shaders/watervert.glsl"),
                new File("src/comp3170/ass3/shaders/waterfrag.glsl")
            );
        } catch (IOException | OpenGLException e) {
            e.printStackTrace();
        }

        // Create OpenGL buffers
        positionBuffer = GLBuffers.createBuffer(positions.toArray(new Vector3f[0]));
        normalBuffer = GLBuffers.createBuffer(normals.toArray(new Vector3f[0]));
        uvBuffer = GLBuffers.createBuffer(uvs.toArray(new Vector2f[0]));
        indexBuffer = GLBuffers.createIndexBuffer(indices.stream().mapToInt(i->i).toArray());
        indexCount = indices.size();
    }

    private void calculateNormals(List<Vector3f> positions, List<Vector3f> normals, 
                                int v1, int v2, int v3) {
        // Get vertices of the triangle
        Vector3f p1 = positions.get(v1);
        Vector3f p2 = positions.get(v2);
        Vector3f p3 = positions.get(v3);

        // Calculate vectors from vertex 1 to vertices 2 and 3
        Vector3f u = new Vector3f(p2).sub(p1);
        Vector3f v = new Vector3f(p3).sub(p1);

        // Calculate cross product for normal
        Vector3f normal = new Vector3f();
        normal.cross(u, v).normalize();

        // Add to existing normals (for smooth shading)
        normals.get(v1).add(normal).normalize();
        normals.get(v2).add(normal).normalize();
        normals.get(v3).add(normal).normalize();
    }

    @Override
    protected void drawSelf(Matrix4f modelMatrix, int pass) {
        if(shader == null) return;

        // Enable blending for transparency
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        shader.enable();
        
        // Set uniforms
        shader.setUniform("u_mvpMatrix", getMVPMatrix(modelMatrix));
        shader.setUniform("u_modelMatrix", modelMatrix);
        shader.setUniform("u_gamma", 2.2f);
        shader.setUniform("u_time", (float)(System.currentTimeMillis() % 100000) / 1000f);
        shader.setUniform("u_debugMode", pass);

        // Camera position for lighting/fresnel
        shader.setUniform("u_cameraPos", cam.getPosition());
        
        // Lighting uniforms
        shader.setUniform("u_cameraPos", cam.getPosition());
        shader.setUniform("u_day", cam.isDay ? 1 : 0);
        shader.setUniform("u_sunDir", cam.sunDir);
        shader.setUniform("u_lampPos", cam.lampPos);
        shader.setUniform("u_lampDir", cam.lampDir);
        shader.setUniform("u_ambient", cam.ambientColor);

        // Set vertex attributes
        shader.setAttribute("a_position", positionBuffer);
        shader.setAttribute("a_normal", normalBuffer);
        shader.setAttribute("a_uv", uvBuffer);
        
        // Draw mesh
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        
        glDisable(GL_BLEND);
    }

    private Matrix4f getMVPMatrix(Matrix4f model) {
        return new Matrix4f()
            .set(cam.getProjectionMatrix())
            .mul(cam.getViewMatrix())
            .mul(model);
    }
}
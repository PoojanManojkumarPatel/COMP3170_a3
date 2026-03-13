package comp3170.ass3.sceneobjects;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL11.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import comp3170.GLBuffers;
import comp3170.InputManager;
import comp3170.OpenGLException;
import comp3170.SceneObject;
import comp3170.Shader;
import comp3170.TextureLibrary;
import comp3170.ass3.models.MeshData;
import comp3170.ass3.models.MeshData.Mesh;

public class Boat extends SceneObject {

	private static final String OBJ_FILE = "src/comp3170/ass3/models/boat.obj";
	private Map<String, Submesh> submeshes = new HashMap<>();
	private String[] keys = {"boat", "fan", "lantern"};
	private SceneObject lanternTransform;

	private Shader shader;
	private Camera cam;
	
	private float fanRotation = 30;
	private float angle = 0;
	private float posX = 50;
	private float posZ = 50;
	private int boatTexture;

	public Boat(Camera cam) {
		this.cam = cam;
		this.getMatrix().identity().translate(50, 22, 50);

		// Load OBJ mesh
		MeshData data = null;
		try {
			data = new MeshData(OBJ_FILE);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		for (String key : keys) {
			Mesh mesh = data.getMesh(key);
			Submesh submesh = new Submesh();
			submesh.vertexBuffer = GLBuffers.createBuffer(mesh.vertices);
			submesh.normalBuffer = GLBuffers.createBuffer(mesh.normals);
			submesh.uvBuffer = GLBuffers.createBuffer(mesh.uvs);
			submesh.indexBuffer = GLBuffers.createIndexBuffer(mesh.indices);
			submesh.indexCount = mesh.indices.length;
			submeshes.put(key, submesh);
			
			if (key.equals("lantern")) {
	            lanternTransform = new SceneObject();
	            lanternTransform.setParent(this);
	        }
		}

		// Load shader
		try {
			shader = new Shader(
				new File("src/comp3170/ass3/shaders/boatvert.glsl"),
				new File("src/comp3170/ass3/shaders/boatfrag.glsl")
			);
		} catch (IOException | OpenGLException e) {
			e.printStackTrace();
		}

		// Load texture
		try {
			boatTexture = TextureLibrary.instance.loadTexture("boat.png");
		} catch (IOException | OpenGLException e) {
			e.printStackTrace();
		}
	}

	private Matrix4f getMVPMatrix(Matrix4f model) {
		return new Matrix4f()
			.set(cam.getProjectionMatrix())
			.mul(cam.getViewMatrix())
			.mul(model);
	}

	@Override
	protected void drawSelf(Matrix4f modelMatrix, int pass) {
	    if(shader == null) return;
	    
	    shader.enable();
	    for (String key : keys) {
	        Submesh submesh = submeshes.get(key);
	        Matrix4f drawMatrix = new Matrix4f(modelMatrix);
	        
	        if (key.equals("fan")) {
	            drawMatrix
	                .translate(0, 1.252717393f, -1.135f)
	                .rotateZ(fanRotation)
	                .translate(0, -1.252717393f, 1.135f);
	        }

	        shader.setUniform("u_mvpMatrix", getMVPMatrix(drawMatrix));
	        shader.setUniform("u_modelMatrix", drawMatrix);
	        shader.setUniform("u_debugMode", pass);
	        shader.setUniform("u_gamma", 2.2f);
	        shader.setUniform("u_texture", 0);
	        
	        // Set whether this mesh is the lantern
	        shader.setUniform("u_isLantern", key.equals("lantern"));
	        
	        // Lighting uniforms
	        shader.setUniform("u_day", cam.isDay ? 1 : 0);
	        shader.setUniform("u_sunDir", cam.sunDir);
	        shader.setUniform("u_lampPos", cam.lampPos);
	        shader.setUniform("u_lampDir", cam.lampDir);
	        shader.setUniform("u_ambient", cam.ambientColor);
	        
	        glActiveTexture(GL_TEXTURE0);
	        glBindTexture(GL_TEXTURE_2D, boatTexture);
	        shader.setAttribute("a_position", submesh.vertexBuffer);
	        shader.setAttribute("a_normal", submesh.normalBuffer);
	        shader.setAttribute("a_uv", submesh.uvBuffer);
	        
	     // Enable blending for the lantern glow
	        if (key.equals("lantern") && !cam.isDay) {
	            glEnable(GL_BLEND);
	            glBlendFunc(GL_SRC_ALPHA, GL_ONE);  // Additive blending for glow
	        }
	        
	        glPolygonMode(GL_FRONT_AND_BACK, pass == 1 ? GL_LINE : GL_FILL);
	        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, submesh.indexBuffer);
	        glDrawElements(GL_TRIANGLES, submesh.indexCount, GL_UNSIGNED_INT, 0);
	        
	        // Disable blending after drawing the lantern
	        if (key.equals("lantern") && !cam.isDay) {
	            glDisable(GL_BLEND);
	        }
	    }
	}

	public void update(float deltaTime, InputManager input) {
	    float speed = 10;
	    float turnSpeed = 4;
	    fanRotation += deltaTime * 10f;

	    if (input.isKeyDown(GLFW.GLFW_KEY_A)) angle += turnSpeed * deltaTime;
	    if (input.isKeyDown(GLFW.GLFW_KEY_D)) angle -= turnSpeed * deltaTime;

	    float dz = 0;
	    if (input.isKeyDown(GLFW.GLFW_KEY_W)) dz += speed * deltaTime;
	    if (input.isKeyDown(GLFW.GLFW_KEY_S)) dz -= speed * deltaTime;

	    posX += dz * (float) Math.sin(angle);
	    posZ += dz * (float) Math.cos(angle);

	    this.getMatrix().identity()
	        .translate(posX, 22, posZ)
	        .rotateY(angle);
	}

	public SceneObject getLanternTransform() {
		return lanternTransform;
	}
}

class Submesh {
	int vertexBuffer;
	int normalBuffer;
	int uvBuffer;
	int indexBuffer;
	int indexCount;
}

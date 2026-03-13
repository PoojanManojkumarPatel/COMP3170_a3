package comp3170.ass3.sceneobjects;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import static org.lwjgl.opengl.GL20.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector2f;

import comp3170.GLBuffers;
import comp3170.OpenGLException;
import comp3170.SceneObject;
import comp3170.Shader;
import comp3170.TextureLibrary;

public class HeightMap extends SceneObject {

	private static final File HEIGHT_MAP = new File("src/comp3170/ass3/maps/islands.png");
	private Shader shader;
	private Camera cam;
	
	private int width;
	private int depth;
	private float[][] heights;
	private int positionBuffer;
	private int normalBuffer;
	private int uvBuffer;
	private int indexBuffer;
	private int indexCount;
	private int grassTex;
	private int sandTex;


	public HeightMap(Camera cam) {
		// load the vertex heights from the image
		loadHeights(HEIGHT_MAP);
		this.cam = cam;
		// Load shader
		try {
			shader = new Shader(
				new File("src/comp3170/ass3/shaders/heightvert.glsl"),
				new File("src/comp3170/ass3/shaders/heightfrag.glsl")
			);
		} catch (IOException | OpenGLException e) {
			e.printStackTrace();
		}
		
		//load texture
		try {
			sandTex = TextureLibrary.instance.loadTexture("terrain-sand.png");
			grassTex = TextureLibrary.instance.loadTexture("terrain-grass.png");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OpenGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		float xScale = 100f / (width - 1);  // total 100m wide
		float zScale = 100f / (depth - 1);  // total 100m deep
		float yScale = 50f;                 // max height = 50m

		List<Vector3f> vertices = new ArrayList<>();
		List<Vector3f> normals = new ArrayList<>();
		List<Vector2f> uvs = new ArrayList<>();
		List<Integer> indices = new ArrayList<>();

		for (int z = 0; z < depth; z++) {
		    for (int x = 0; x < width; x++) {
		        float y = heights[x][z] * yScale;
		        // Center the terrain around origin
		        float xPos = x * xScale;
		        float zPos = z * zScale;
		        vertices.add(new Vector3f(xPos, y, zPos));
		        
		        // UV coordinates scaled to terrain size
		        uvs.add(new Vector2f(xPos/100f, zPos/100f));  // Normalize to [0,1]
		        
		        normals.add(computeNormal(x,z,xScale,zScale,yScale));
		    }
		}

		for (int z = 0; z < depth - 1; z++) {
		    for (int x = 0; x < width - 1; x++) {
		        int i0 = z * width + x;
		        int i1 = i0 + 1;
		        int i2 = i0 + width;
		        int i3 = i2 + 1;

		        indices.add(i0); indices.add(i2); indices.add(i1); // triangle 1
		        indices.add(i1); indices.add(i2); indices.add(i3); // triangle 2
		    }
		}
		
		positionBuffer = GLBuffers.createBuffer(vertices.toArray(new Vector3f[0]));
		normalBuffer = GLBuffers.createBuffer(normals.toArray(new Vector3f[0]));
		uvBuffer = GLBuffers.createBuffer(uvs.toArray(new Vector2f[0]));

		int[] indexArray = indices.stream().mapToInt(i -> i).toArray();
		indexBuffer = GLBuffers.createIndexBuffer(indexArray);
		indexCount = indexArray.length;
		
	}
	
	private float getHeight(int x, int z) {
	    // Clamp to valid range to prevent ArrayIndexOutOfBoundsException
	    x = Math.max(0, Math.min(x, width - 1));
	    z = Math.max(0, Math.min(z, depth - 1));

	    return heights[x][z]; // already normalized between 0 and 1
	}

	
	private Vector3f computeNormal(int x, int z, float xScale, float zScale, float yScale) {
	    // Get heights of surrounding vertices
	    float yP = getHeight(x, z + 1) * yScale;  // North
	    float yM = getHeight(x, z - 1) * yScale;  // South
	    float xP = getHeight(x + 1, z) * yScale;  // East
	    float xM = getHeight(x - 1, z) * yScale;  // West
	    
	    // Calculate normal using central differences for smoother normals
	    // This creates a more accurate normal by considering all four neighbors
	    float dx = (xP - xM) / (2.0f * xScale);
	    float dz = (yP - yM) / (2.0f * zScale);
	    
	    // The normal is (-dx, 1, -dz) normalized
	    // The negative signs are because in OpenGL, increasing z goes into the screen
	    Vector3f normal = new Vector3f(-dx, 1.0f, -dz).normalize();
	    
	    return normal;
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
	    
	    // Enable shader and set matrices
	    shader.enable();
	    shader.setUniform("u_modelMatrix", modelMatrix);
	    shader.setUniform("u_mvpMatrix", getMVPMatrix(modelMatrix));
	    
	    // Set camera position
	    shader.setUniform("u_cameraPos", cam.getPosition());
	    
	    // Debug and visual parameters
	    shader.setUniform("u_debugMode", pass);
	    shader.setUniform("u_gamma", 2.2f);
	    shader.setUniform("u_waterLevel", 22f);
	    shader.setUniform("u_blendMargin", 2f);
	    
	 // Bind textures before setting uniforms
	    glActiveTexture(GL_TEXTURE0);
	    glBindTexture(GL_TEXTURE_2D, grassTex);
	    glActiveTexture(GL_TEXTURE1);
	    glBindTexture(GL_TEXTURE_2D, sandTex);
	    
	    
	    shader.setUniform("u_grassTex", 0);  // Use texture unit 0
	    shader.setUniform("u_sandTex", 1);   // Use texture unit 1
	    
	    // Lighting uniforms
	    shader.setUniform("u_day", cam.isDay ? 1 : 0);
	    shader.setUniform("u_sunDir", cam.sunDir);
	    shader.setUniform("u_lampPos", cam.lampPos);
	    shader.setUniform("u_lampDir", cam.lampDir);
	    shader.setUniform("u_ambient", cam.ambientColor);
	    
	    // Set attributes
	    shader.setAttribute("a_position", positionBuffer);
	    shader.setAttribute("a_normal", normalBuffer);
	    shader.setAttribute("a_uv", uvBuffer);
	    
	    // Draw
	    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
	    glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
	}




	/**
	 * Load the height data. This sets the values of the fields:
	 *
	 *    (width, depth) are set to the (x,y) image size, 
	 *    which should equal the number of vertices in each direction
	 *    
	 *    heights[i][j] is set to the value of the pixel at coordinates (i,j).
	 *    This is a float between 0 and 1, corresponding to the minimum and maximum height values. 
	 * 
	 * @param imageFile
	 */
	
	private void loadHeights(File imageFile) {
		BufferedImage mapImage;
		try {
			mapImage = ImageIO.read(imageFile);

			width = mapImage.getWidth();
			depth = mapImage.getHeight();

			heights = new float[width][depth];
			int n = 0;

			for (int x = 0; x < width; x++) {
				for (int z = 0; z < depth; z++) {
					int rgb = mapImage.getRGB(x, z);
					int r = (rgb & 0xff0000) >> 16;
					int g = (rgb & 0xff00) >> 8;
					int b = rgb & 0xff;

					// scale to [0...1]
					heights[x][z] = (r + g + b) / 255f / 3;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

}

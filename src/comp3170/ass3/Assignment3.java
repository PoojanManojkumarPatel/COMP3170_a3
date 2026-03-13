package comp3170.ass3;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL32.GL_PROGRAM_POINT_SIZE;

import java.io.File;

import comp3170.IWindowListener;
import comp3170.InputManager;
import comp3170.OpenGLException;
import comp3170.ShaderLibrary;
import comp3170.TextureLibrary;
import comp3170.Window;

public class Assignment3 implements IWindowListener {

	public static Assignment3 theWindow;
	private static final File SHADER_DIR = new File("src/comp3170/ass3/shaders");
	private static final File TEXTURE_DIR = new File("src/comp3170/ass3/textures");

	private Window window;
	private int screenWidth = 800;
	private int screenHeight = 800;
	private Scene scene;

	private InputManager input;
	private long oldTime;

	public Assignment3() throws OpenGLException {
		window = new Window("Assignment3", screenWidth, screenHeight, this);
		window.setSamples(4); // enable 4x multisampling (anti-aliasing)
		window.run();
	}

	@Override
	public void init() {
		glClearColor(0.53f, 0.81f, 0.98f, 1.0f); // set default sky colour (day mode)
		glEnable(GL_DEPTH_TEST);                // enable depth testing
		glEnable(GL_CULL_FACE);                 // enable backface culling
		glCullFace(GL_BACK);
		glDepthFunc(GL_LEQUAL);                 // allow overlapping fragments at same depth
		glEnable(GL_MULTISAMPLE);               // enable multisample anti-aliasing

		new ShaderLibrary(SHADER_DIR);          // load all shaders into library
		new TextureLibrary(TEXTURE_DIR);        // load all textures into library
		scene = new Scene();

		input = new InputManager(window);
		oldTime = System.currentTimeMillis();   // store initial time
	}

	private void update() {
		long time = System.currentTimeMillis();
		float deltaTime = (time - oldTime) / 1000f;
		oldTime = time;

		scene.update(deltaTime, input); // update scene logic
		input.clear();                  // reset input state after update
	}

	@Override
	public void draw() {
		update();

		// update background colour based on day/night state
		if (scene.gettime()) glClearColor(0.53f, 0.81f, 0.98f, 1.0f); // day
		else glClearColor(0.2f, 0.2f, 0.2f, 1.0f);                    // night

		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);

		scene.draw(); // render all objects in scene

		glFinish(); // ensure all OpenGL commands complete
	}

	@Override
	public void resize(int width, int height) {
		scene.getCamera().setAspect((float) width / height); // update camera aspect ratio
	}

	@Override
	public void close() {
		// no cleanup required
	}

	public static void main(String[] args) throws OpenGLException {
		new Assignment3(); // launch the window and scene
	}
}

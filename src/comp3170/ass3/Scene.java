package comp3170.ass3;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import comp3170.InputManager;
import comp3170.SceneObject;
import comp3170.ass3.sceneobjects.Boat;
import comp3170.ass3.sceneobjects.Camera;
import comp3170.ass3.sceneobjects.HeightMap;
import comp3170.ass3.sceneobjects.Water;

public class Scene extends SceneObject {
    private Boat boat;
    private Camera camera;
    private HeightMap map;
    private Water water;

    public enum DebugMode {
        DEFAULT,
        WIREFRAME,
        NORMALS,
        UV
    }

    private DebugMode debugMode = DebugMode.DEFAULT;

    public Scene() {
        camera = new Camera();
        camera.setAspect(1);
        camera.setNear(0.1f);
        camera.setFar(200f);
        camera.setFOV((float)Math.toRadians(60));

        map = new HeightMap(camera);
        map.setParent(this);

        water = new Water(camera);
        water.setParent(this);

        boat = new Boat(camera);
        boat.setParent(this);

        camera.setTarget(boat); // camera follows the boat in third-person
    }

    public void update(float deltaTime, InputManager input) {
        camera.update(input, deltaTime);
        boat.update(deltaTime, input);

        // Toggle debug modes
        if (input.wasKeyPressed(GLFW.GLFW_KEY_V)) debugMode = DebugMode.DEFAULT;
        if (input.wasKeyPressed(GLFW.GLFW_KEY_B)) debugMode = DebugMode.WIREFRAME;
        if (input.wasKeyPressed(GLFW.GLFW_KEY_N)) debugMode = DebugMode.NORMALS;
        if (input.wasKeyPressed(GLFW.GLFW_KEY_M)) debugMode = DebugMode.UV;

        // Toggle day/night mode
        if (input.wasKeyPressed(GLFW.GLFW_KEY_P)) camera.isDay = !camera.isDay;

        // Rotate light direction (sun or lantern) based on time mode
        if (camera.isDay) {
            if (input.isKeyDown(GLFW.GLFW_KEY_LEFT_BRACKET)) camera.sunAngle -= deltaTime;
            if (input.isKeyDown(GLFW.GLFW_KEY_RIGHT_BRACKET)) camera.sunAngle += deltaTime;
        } else {
            if (input.isKeyDown(GLFW.GLFW_KEY_LEFT_BRACKET)) camera.lampYaw -= deltaTime;
            if (input.isKeyDown(GLFW.GLFW_KEY_RIGHT_BRACKET)) camera.lampYaw += deltaTime;
        }

        // Update lighting using boat’s lantern position
        camera.updateLighting(boat.getLanternTransform());
    }

    @Override
    public void draw() {
        super.draw(new Matrix4f(), debugMode.ordinal());
    }

    public Camera getCamera() {
        return camera;
    }

    public boolean gettime() {
        return camera.isDay;
    }
}

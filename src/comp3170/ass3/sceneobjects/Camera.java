package comp3170.ass3.sceneobjects;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import comp3170.InputManager;
import comp3170.SceneObject;
import static org.lwjgl.glfw.GLFW.*;

public class Camera {
    // View frustum parameters
    private float fov = (float)Math.toRadians(60);
    private float aspect = 1;
    private float near = 1f;
    private float far = 1000f;
    
    // Matrices
    private Matrix4f projectionMatrix = new Matrix4f();
    private Matrix4f viewMatrix = new Matrix4f();
    
    // Camera position and orientation
    private Vector3f cameraPos = new Vector3f();
    public float yaw = 0;
    public float pitch = 0;
    public float distance = 10;
    public float height = 3;
    
    // Camera constraints
    private final float MIN_DISTANCE = 3f;
    private final float MAX_DISTANCE = 50f;
    private final float MAX_PITCH = (float)Math.toRadians(80);
    private final float MIN_FOV = (float)Math.toRadians(30);
    private final float MAX_FOV = (float)Math.toRadians(90);
    
    // Movement smoothing
    private float targetYaw = 0;
    private float targetPitch = 0;
    private float targetDistance = 10;
    private final float SMOOTH_FACTOR = 5f;
    
    // Movement speeds
    private final float ROTATION_SPEED = 1.5f;
    private final float DISTANCE_SPEED = 12f;
    private final float FOV_SPEED = 1.0f;
    
    private SceneObject target;
    
    // Lighting state (public)
    public boolean isDay = true;
    public float sunAngle = 0;
    public float lampYaw = 0;
    public final Vector3f sunDir = new Vector3f();
    public final Vector3f lampDir = new Vector3f();
    public final Vector3f lampPos = new Vector3f();
    public final Vector3f ambientColor = new Vector3f();

    public Camera() {
        updateProjectionMatrix();
    }

    // ---- Projection Matrix Methods ----
    public void updateProjectionMatrix() {
        projectionMatrix.identity().perspective(fov, aspect, near, far);
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public void setAspect(float aspect) {
        this.aspect = aspect;
        updateProjectionMatrix();
    }

    public void setNear(float near) {
        this.near = near;
        updateProjectionMatrix();
    }

    public void setFar(float far) {
        this.far = far;
        updateProjectionMatrix();
    }

    public void setFOV(float fov) {
        this.fov = Math.max(MIN_FOV, Math.min(MAX_FOV, fov));
        updateProjectionMatrix();
    }

    public void adjustFOV(float delta) {
        setFOV(fov + delta);
    }

    // ---- Camera Movement Methods ----
    public void setTarget(SceneObject target) {
        this.target = target;
    }

    public Vector3f getPosition() {
        return new Vector3f(cameraPos);
    }

    public void adjustYaw(float delta) {
        targetYaw += delta;
    }

    public void adjustPitch(float delta) {
        targetPitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, targetPitch + delta));
    }

    public void adjustDistance(float delta) {
        targetDistance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, targetDistance + delta));
    }

    private float lerp(float current, float target, float factor) {
        return current + (target - current) * factor;
    }

    private void smoothUpdate(float deltaTime) {
        float smoothing = SMOOTH_FACTOR * deltaTime;
        yaw = lerp(yaw, targetYaw, smoothing);
        pitch = lerp(pitch, targetPitch, smoothing);
        distance = lerp(distance, targetDistance, smoothing);
    }

    public void update(InputManager input, float deltaTime) {
        // Camera rotation (yaw)
        if (input.isKeyDown(GLFW_KEY_LEFT)) adjustYaw(ROTATION_SPEED * deltaTime);
        if (input.isKeyDown(GLFW_KEY_RIGHT)) adjustYaw(-ROTATION_SPEED * deltaTime);
        
        // Camera pitch
        if (input.isKeyDown(GLFW_KEY_UP)) adjustPitch(ROTATION_SPEED * deltaTime);
        if (input.isKeyDown(GLFW_KEY_DOWN)) adjustPitch(-ROTATION_SPEED * deltaTime);
        
        // Camera distance
        if (input.isKeyDown(GLFW_KEY_U)) adjustDistance(-DISTANCE_SPEED * deltaTime);
        if (input.isKeyDown(GLFW_KEY_I)) adjustDistance(DISTANCE_SPEED * deltaTime);
        
        // FOV adjustment
        if (input.isKeyDown(GLFW_KEY_COMMA)) adjustFOV(-FOV_SPEED * deltaTime);
        if (input.isKeyDown(GLFW_KEY_PERIOD)) adjustFOV(FOV_SPEED * deltaTime);
        
        // Apply smooth movement
        smoothUpdate(deltaTime);
        
        // Update view matrix
        updateViewMatrix();
    }

    public void updateViewMatrix() {
        if (target == null) return;
        
        // Get target position
        Matrix4f modelToWorld = new Matrix4f();
        target.getModelToWorldMatrix(modelToWorld);
        Vector3f targetPos = modelToWorld.transformPosition(new Vector3f(0, 0, 0));
        
        // Calculate camera position using spherical coordinates
        float horizontalDistance = distance * (float)Math.cos(pitch);
        float verticalDistance = distance * (float)Math.sin(pitch);
        
        float dx = horizontalDistance * (float)Math.sin(yaw);
        float dz = horizontalDistance * (float)Math.cos(yaw);
        
        // Set camera position
        cameraPos.set(
            targetPos.x - dx,
            targetPos.y + height + verticalDistance,
            targetPos.z - dz
        );
        
        // Update view matrix
        viewMatrix.identity().lookAt(
            cameraPos,           // Camera position
            targetPos,           // Look at target
            new Vector3f(0, 1, 0) // Up vector
        );
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

 // ---- Lighting Methods ----
    public void updateLighting(SceneObject lampBase) {
        // Update sun direction - notice the Y component is positive for downward light
        sunDir.set((float)Math.sin(sunAngle), 1.0f, (float)Math.cos(sunAngle)).normalize();
        
        // Update ambient light based on time of day
        ambientColor.set(isDay ? 0.3f : 0.05f);

        if (lampBase != null) {
            // Get the lamp's world transformation matrix
            Matrix4f lampMatrix = new Matrix4f();
            lampBase.getModelToWorldMatrix(lampMatrix);
            
            // Update lamp position in world space
            Vector3f localLampPos = new Vector3f(-0.78f, 1.39f, 0.58f);
            lampMatrix.transformPosition(localLampPos, lampPos);
            
            // Calculate lamp direction based on both boat rotation and lamp yaw
            Vector3f forward = new Vector3f(0, 0, 1);  // Forward direction
            lampMatrix.transformDirection(forward);     // Transform by boat rotation
            
            // Apply additional lamp yaw rotation
            float cosYaw = (float)Math.cos(lampYaw);
            float sinYaw = (float)Math.sin(lampYaw);
            lampDir.set(
                forward.x * cosYaw - forward.z * sinYaw,
                0,  // Keep Y component 0 to make lamp point horizontally
                forward.x * sinYaw + forward.z * cosYaw
            ).normalize();
        }
    }
}
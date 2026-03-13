#version 410

in vec3 a_position;
in vec3 a_normal;
in vec2 a_uv;

uniform mat4 u_modelMatrix;
uniform mat4 u_mvpMatrix;
uniform float u_time;

out vec3 v_worldPos;
out vec3 v_normal;
out vec2 v_uv;

const float WAVE_SPEED = 1.0;
const float WAVE_LENGTH = 1.0;
const float WAVE_HEIGHT = 0.1;

void main() {
    // Calculate wave displacement
    float wave1 = sin(a_position.x * WAVE_LENGTH + u_time * WAVE_SPEED);
    float wave2 = sin(a_position.z * WAVE_LENGTH * 0.7 + u_time * WAVE_SPEED * 1.3);
    vec3 position = a_position;
    position.y += (wave1 + wave2) * WAVE_HEIGHT;

    // Transform positions
    vec4 worldPos = u_modelMatrix * vec4(position, 1.0);
    gl_Position = u_mvpMatrix * vec4(position, 1.0);

    // Pass to fragment shader
    v_worldPos = worldPos.xyz;
    v_normal = normalize(mat3(u_modelMatrix) * a_normal);
    v_uv = a_uv;
}
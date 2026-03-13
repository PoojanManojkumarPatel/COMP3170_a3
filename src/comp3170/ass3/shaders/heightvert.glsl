#version 410
in vec3 a_position;
in vec3 a_normal;
in vec2 a_uv;

uniform mat4 u_modelMatrix;
uniform mat4 u_mvpMatrix;

out vec3 v_worldPos;
out vec3 v_normal;
out vec2 v_uv;

void main() {
    gl_Position = u_mvpMatrix * vec4(a_position, 1.0);
    v_normal = mat3(u_modelMatrix) * a_normal;
    v_uv = a_uv;
    v_worldPos = (u_modelMatrix * vec4(a_position, 1.0)).xyz;
}
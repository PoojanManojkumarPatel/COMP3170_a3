#version 410
in vec4 a_position;
in vec4 a_normal;
in vec2 a_uv;

uniform mat4 u_mvpMatrix;
uniform mat4 u_modelMatrix;

out vec3 v_normal;
out vec2 v_uv;
out vec3 v_worldPos;

void main() {
    gl_Position = u_mvpMatrix * a_position;
    v_normal = mat3(u_modelMatrix) * a_normal.xyz;
    v_uv = a_uv;
    v_worldPos = (u_modelMatrix * a_position).xyz;
}
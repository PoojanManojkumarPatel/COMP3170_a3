#version 410
in vec3 v_normal;
in vec2 v_uv;
in vec3 v_worldPos;

// Add camera position uniform
uniform vec3 u_cameraPos;
uniform int u_day;
uniform vec3 u_sunDir;
uniform vec3 u_lampDir;
uniform vec3 u_lampPos;
uniform vec3 u_ambient;
uniform int u_debugMode;
uniform float u_gamma;
uniform sampler2D u_grassTex;
uniform sampler2D u_sandTex;
uniform float u_waterLevel;
uniform float u_blendMargin;

out vec4 o_colour;

const float TEXTURE_SCALE = 10.0;
const float SPECULAR_POWER = 32.0;

void main() {
    // Debug visualization modes
    if (u_debugMode == 1) {
        // Wireframe mode (keep as is)
        vec3 normalColor = normalize(v_normal) * 0.5 + 0.5;
        o_colour = vec4(pow(normalColor, vec3(1.0 / u_gamma)), 1.0);
        return;
    }
    
    if (u_debugMode == 2) {
        // Normal visualization - show actual normal colors
        vec3 normalColor = normalize(v_normal) * 0.5 + 0.5;
        o_colour = vec4(normalColor, 1.0);  // Remove gamma correction for debug
        return;
    }

    if (u_debugMode == 3) {
        // UV visualization - show actual UV colors
        vec2 wrappedUV = mod(v_uv, 1.0);
        o_colour = vec4(wrappedUV.x, wrappedUV.y, 0.0, 1.0);  // Remove gamma correction for debug
        return;
    }


    // Calculate view direction
    vec3 viewDir = normalize(u_cameraPos - v_worldPos);
    vec3 normal = normalize(v_normal);

    // Height-based texture blending
    float height = v_worldPos.y;
    float blend = smoothstep(u_waterLevel - u_blendMargin, 
                           u_waterLevel + u_blendMargin, 
                           height);

    // Sample textures with proper scaling
    vec3 sand = texture(u_sandTex, v_uv * TEXTURE_SCALE).rgb;
    vec3 grass = texture(u_grassTex, v_uv * TEXTURE_SCALE).rgb;
    vec3 baseColor = mix(sand, grass, blend);

    // Initialize lighting
    vec3 lighting = u_ambient;

    if (u_day == 1) {
        // Daytime lighting (sun)
        vec3 lightDir = normalize(u_sunDir);
        
        // Diffuse lighting
        float diff = max(dot(normal, lightDir), 0.0);
        lighting += diff * vec3(1.0);
        
        // Specular highlight
        vec3 reflectDir = reflect(-lightDir, normal);
        float spec = pow(max(dot(viewDir, reflectDir), 0.0), SPECULAR_POWER);
        lighting += vec3(0.3) * spec;
        
    } else {
        // Night lighting (lamp)
        vec3 toLight = u_lampPos - v_worldPos;
        float distance = length(toLight);
        vec3 lightDir = normalize(toLight);
        
        // Spotlight cone calculation
        float theta = dot(lightDir, normalize(-u_lampDir));
        float epsilon = 0.1;
        float intensity = smoothstep(0.0, 1.0, (theta - 0.7) / epsilon);
        
        if (intensity > 0.0) {
            // Diffuse
            float diff = max(dot(normal, lightDir), 0.0);
            
            // Specular
            vec3 reflectDir = reflect(-lightDir, normal);
            float spec = pow(max(dot(viewDir, reflectDir), 0.0), SPECULAR_POWER);
            
            // Distance attenuation (softer falloff)
            float attenuation = 1.0 / (1.0 + 0.05 * distance + 0.005 * distance * distance);
            
            // Combine lighting components
            lighting += (diff * vec3(1.0) + spec * vec3(0.3)) * attenuation * intensity;
        }
    }

    // Ensure minimum ambient light
    lighting = max(lighting, vec3(0.1));

    // Apply lighting and gamma correction
    vec3 finalColor = baseColor * lighting;
    finalColor = pow(finalColor, vec3(1.0 / u_gamma));
    
    o_colour = vec4(finalColor, 1.0);
}
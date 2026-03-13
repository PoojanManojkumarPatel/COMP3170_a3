#version 410
in vec3 v_normal;
in vec2 v_uv;
in vec3 v_worldPos;

uniform sampler2D u_texture;
uniform int u_day;
uniform vec3 u_sunDir;
uniform vec3 u_lampPos;
uniform vec3 u_lampDir;
uniform vec3 u_ambient;
uniform float u_gamma;
uniform int u_debugMode;
uniform bool u_isLantern;

out vec4 o_colour;

void main() {
    // Debug modes
    if (u_debugMode == 1) {
        // Wireframe mode - solid black
        o_colour = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    if (u_debugMode == 2) {
        // Normal visualization - full RGB range
        vec3 n = normalize(v_normal);
        vec3 normalColor = n * 0.5 + 0.5;  // Transform from [-1,1] to [0,1]
        o_colour = vec4(normalColor, 1.0);
        return;
    }
    if (u_debugMode == 3) {
        // UV visualization - vibrant colors
        vec2 uv = fract(v_uv);  // Ensure UVs are wrapped to [0,1]
        o_colour = vec4(uv.x, uv.y, 0.0, 1.0);
        return;
    }

    // Regular rendering
    vec3 normal = normalize(v_normal);
    vec3 texColor = texture(u_texture, v_uv).rgb;
    vec3 lighting = u_ambient;

    // Lantern glow at night
    if (u_isLantern && u_day == 0) {
        vec3 glowColor = vec3(1.0, 0.9, 0.7); // Warm yellow glow
        o_colour = vec4(glowColor, 1.0);
        return;
    }

    if (u_day == 1) {
        // Day lighting
        float sunDiffuse = max(dot(normal, normalize(u_sunDir)), 0.0);
        lighting += sunDiffuse;
    } else {
        // Night lighting with improved lamp effect
        vec3 toLight = u_lampPos - v_worldPos;
        float distance = length(toLight);
        vec3 lightDir = normalize(toLight);
        
        // Spotlight cone calculation
        float spotEffect = dot(normalize(-u_lampDir), lightDir);
        float spotCutoff = 0.7;
        float spotOuterCone = 0.6;
        
        if (spotEffect > spotOuterCone) {
            // Spotlight intensity with soft edges
            float spotIntensity = smoothstep(spotOuterCone, spotCutoff, spotEffect);
            
            // Enhanced attenuation for better light falloff
            float maxDistance = 30.0;
            float attenuation = max(0.0, 1.0 - (distance / maxDistance));
            attenuation = attenuation * attenuation;
            
            // Warm light color
            vec3 lampColor = vec3(1.0, 0.9, 0.7);
            
            float diffuse = max(dot(normal, lightDir), 0.0);
            
            // Add specular reflection for the boat
            vec3 viewDir = normalize(-v_worldPos);
            vec3 halfwayDir = normalize(lightDir + viewDir);
            float spec = pow(max(dot(normal, halfwayDir), 0.0), 32.0);
            vec3 specular = spec * lampColor * 0.5;
            
            lighting += (diffuse * attenuation * spotIntensity * lampColor * 2.0) + specular;
        }
    }

    vec3 finalColor = texColor * lighting;
    // Apply gamma correction
    finalColor = pow(finalColor, vec3(1.0 / u_gamma));
    o_colour = vec4(finalColor, 1.0);
}
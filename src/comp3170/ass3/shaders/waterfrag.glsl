#version 410

uniform vec3 u_cameraPos;
uniform vec3 u_sunDir;
uniform vec3 u_ambient;
uniform float u_gamma;
uniform vec3 u_lampDir;
uniform vec3 u_lampPos;
uniform float u_time;
uniform int u_day;
uniform int u_debugMode;

in vec3 v_worldPos;
in vec3 v_normal;
in vec2 v_uv;

out vec4 fragColor;

const float rippleSpeed = 2.0;
const float rippleAmp = 0.15;
const float rippleFreq = 8.0;

vec3 applyGamma(vec3 color) {
    return pow(color, vec3(1.0/u_gamma));
}

void main() {
    // Debug modes
    if (u_debugMode == 1) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.4);
        return;
    }

    if (u_debugMode == 2) {
        vec3 normal = normalize(v_normal);
        fragColor = vec4(normal * 0.5 + 0.5, 0.6);
        return;
    }

    if (u_debugMode == 3) {
        vec2 wrappedUV = mod(v_uv, 1.0);
        fragColor = vec4(vec3(wrappedUV, 0.0), 0.6);
        return;
    }

    // Base water color
    vec3 baseColor = vec3(0.0, 0.3, 0.5);
    
    // Calculate ripple effect
    float wave1 = sin(v_uv.x * rippleFreq + u_time * rippleSpeed) *
                  cos(v_uv.y * rippleFreq + u_time * rippleSpeed);
    float wave2 = sin(v_uv.x * rippleFreq * 1.5 - u_time * rippleSpeed * 0.8) *
                  cos(v_uv.y * rippleFreq * 1.5 - u_time * rippleSpeed * 0.8);
    float wave = (wave1 + wave2) * 0.5;
    
    // Create ripple normal
    vec3 rippleNormal = normalize(v_normal + vec3(wave * rippleAmp, 0.0, wave * rippleAmp));
    
    // View direction for specular calculations
    vec3 viewDir = normalize(u_cameraPos - v_worldPos);
    
    float sunDiffuse = 0.0;
    float lampDiffuse = 0.0;
    
    // Daytime lighting
    if (u_day == 1) {
        vec3 lightDir = normalize(u_sunDir);
        
        // Diffuse reflection
        sunDiffuse = max(dot(lightDir, rippleNormal), 0.0);
        
        // Specular reflection with Fresnel effect
        vec3 reflectDir = reflect(-lightDir, rippleNormal);
        float spec = pow(max(dot(viewDir, reflectDir), 0.0), 64.0); // Increased shininess
        float fresnelTerm = pow(1.0 - max(dot(viewDir, rippleNormal), 0.0), 4.0);
        float specular = spec * fresnelTerm * 0.8; // Adjusted strength
        
        // Dynamic reflection based on view angle
        float viewDependentReflection = pow(1.0 - abs(dot(viewDir, rippleNormal)), 2.0);
        
        // Combine lighting components
        vec3 finalColor = baseColor * (sunDiffuse + u_ambient) +
                         vec3(specular) * viewDependentReflection;
        
        // Apply gamma correction before alpha
        finalColor = applyGamma(finalColor);
        
        // Apply water transparency
        float alpha = mix(0.6, 0.9, fresnelTerm);
        fragColor = vec4(finalColor, alpha);
    }
    // Night-time lighting
    else {
        vec3 toFrag = normalize(v_worldPos - u_lampPos);
        float spotlight = dot(u_lampDir, toFrag);
        
        if (spotlight > 0.7) {
            // Lamp diffuse with distance attenuation
            float distance = length(u_lampPos - v_worldPos);
            float attenuation = 1.0 / (1.0 + 0.1 * distance + 0.01 * distance * distance);
            lampDiffuse = max(dot(rippleNormal, -toFrag), 0.0) * 0.3 * attenuation;
            
            // Lamp specular
            vec3 reflectDir = reflect(toFrag, rippleNormal);
            float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);
            float specular = spec * 0.4 * attenuation;
            
            vec3 finalColor = baseColor * (lampDiffuse + u_ambient) + vec3(specular);
            finalColor = applyGamma(finalColor);
            fragColor = vec4(finalColor, 0.8);
        }
        else {
            vec3 finalColor = baseColor * u_ambient;
            finalColor = applyGamma(finalColor);
            fragColor = vec4(finalColor, 0.7);
        }
    }
}
uniform sampler2D screenTex;

// 1-0
uniform float intensity;

// secs after game start
uniform float time;

// IMPORTANT
// makes the color picker much less colorful.
uniform int colorSensitivityMode;

// updates once per frame, usually
const float GLITCH_FPS = 60.0;

// max sideways shift, in % of screen width
const float MIN_OFFSET = 0.02;
const float MAX_OFFSET = 0.10;

// scales with intensity
const float MIN_TINT_MULT = 0.7;
const float MAX_TINT_MULT = 2.5;

const float CHANCE_MULT = 0.3;

const int LAYERS = 3;

float random01(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 23.2232133))) * 87174.68272);
}

vec3 pickTint(float id) {
    if (colorSensitivityMode == 1) {
        return vec3(0.0, 0.0, 0.0);
    }
    if (id < 0.33) return vec3(1.0, 0.0, 0.0); // red
    else if (id < 0.66) return vec3(0.0, 1.0, 0.0); // green
    else return (vec3(0.0, 0.0, 1.0)); // blue
}

void tryGlitch(vec2 uv, float bandCount, float layerSeed, float tick,
        inout float shift, inout vec3 tint, inout float glitchMult) {

    float band = floor(uv.y * bandCount);
    vec2 seed = vec2(band + layerSeed * 17.0, tick + layerSeed * 53.0);

    if (intensity <= 1 && random01(seed + 23.251) < 0.5) {
        return;
    }
    float chance = (intensity * CHANCE_MULT) / float(LAYERS);
    float roll = random01(seed);
    if (roll >= chance) {
        return;
    }

    float dist = mix(MIN_OFFSET, MAX_OFFSET, random01(seed + 11.7));
    float dir = (random01(seed + 23.1) < 0.5) ? -1.0 : 1.0;

    shift = dir * dist;
    tint = pickTint(random01(seed + 37.4));
    glitchMult = 1.0;
}

void main() {
    vec2 uv = gl_TexCoord[0].xy;

    // so its not based on fps
    float tick = floor(time * GLITCH_FPS);

    float shift = 0.0;
    vec3 tint = vec3(1.0);
    float glitchMult = 0.0;

    int left = LAYERS;
    float size = 8.0;
    while (left-- > 0) {
        tryGlitch(uv, size, float(left), tick, shift, tint, glitchMult);
        size *= 3.0;
    }

    vec2 modifiedUV = vec2(uv.x + shift, uv.y);

    vec4 color = texture2D(screenTex, modifiedUV);

    float tintAmount = mix(MIN_TINT_MULT, MAX_TINT_MULT, intensity);
    color.r = mix(color.r, color.r + tint.r * 0.15, tintAmount * glitchMult);
    color.g = mix(color.g, color.g + tint.g * 0.15, tintAmount * glitchMult);
    color.b = mix(color.b, color.b + tint.b * 0.15, tintAmount * glitchMult);

    gl_FragColor = color;
}

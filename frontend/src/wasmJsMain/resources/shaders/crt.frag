precision mediump float;

uniform sampler2D frameTexture;
uniform vec2 outputSize;
uniform float time;

varying vec2 vTexCoord;

const vec2 sourceSize = vec2(256.0, 240.0);

float random(vec2 point) {
    return fract(sin(dot(point, vec2(12.9898, 78.233))) * 43758.5453);
}

vec3 linearSample(vec2 coordinate) {
    return pow(texture2D(frameTexture, coordinate).rgb, vec3(2.2));
}

vec3 analogSignal(vec2 coordinate) {
    vec2 pixel = 1.0 / sourceSize;
    vec3 farLeft = linearSample(coordinate - vec2(1.65 * pixel.x, 0.0));
    vec3 left = linearSample(coordinate - vec2(0.70 * pixel.x, 0.0));
    vec3 center = linearSample(coordinate);
    vec3 right = linearSample(coordinate + vec2(0.70 * pixel.x, 0.0));
    vec3 farRight = linearSample(coordinate + vec2(1.65 * pixel.x, 0.0));

    vec3 signal;
    signal.r = farLeft.r * 0.07 +
        left.r * 0.24 +
        center.r * 0.45 +
        right.r * 0.20 +
        farRight.r * 0.04;
    signal.g = farLeft.g * 0.04 +
        left.g * 0.20 +
        center.g * 0.52 +
        right.g * 0.20 +
        farRight.g * 0.04;
    signal.b = farLeft.b * 0.04 +
        left.b * 0.20 +
        center.b * 0.45 +
        right.b * 0.24 +
        farRight.b * 0.07;
    return signal;
}

vec3 phosphorMask() {
    float row = mod(floor(gl_FragCoord.y / 2.0), 2.0);
    float column = mod(floor(gl_FragCoord.x / 2.0) + row, 3.0);
    vec3 mask = vec3(0.78);
    if (column < 1.0) {
        mask.r = 1.14;
    } else if (column < 2.0) {
        mask.g = 1.14;
    } else {
        mask.b = 1.14;
    }
    return mask * mix(1.0, 0.94, row);
}

void main() {
    vec2 glassPoint = vTexCoord * 2.0 - 1.0;
    vec2 sampleCoordinate = glassPoint * 0.965 * 0.5 + 0.5;

    vec3 color = analogSignal(sampleCoordinate);
    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));

    float linePosition = abs(fract(sampleCoordinate.y * sourceSize.y) - 0.5) * 2.0;
    float beamWidth = mix(0.34, 0.72, smoothstep(0.04, 0.82, luminance));
    float beam = exp(-(linePosition * linePosition) / (2.0 * beamWidth * beamWidth));
    color *= 0.54 + 0.59 * beam;

    vec2 bloomPixel = 1.0 / sourceSize;
    vec3 bloom = linearSample(sampleCoordinate + vec2(0.0, bloomPixel.y)) +
        linearSample(sampleCoordinate - vec2(0.0, bloomPixel.y)) +
        linearSample(sampleCoordinate + vec2(2.0 * bloomPixel.x, 0.0)) +
        linearSample(sampleCoordinate - vec2(2.0 * bloomPixel.x, 0.0));
    bloom *= 0.25;
    color += max(bloom - vec3(0.24), 0.0) * 0.075;

    float maskStrength = smoothstep(640.0, 1050.0, outputSize.x);
    color *= mix(vec3(1.0), phosphorMask(), 0.72 * maskStrength);

    float vignette = 16.0 * sampleCoordinate.x * sampleCoordinate.y *
        (1.0 - sampleCoordinate.x) * (1.0 - sampleCoordinate.y);
    vignette = pow(max(vignette, 0.0), 0.17);
    color *= 0.68 + 0.32 * vignette;

    float noise = random(gl_FragCoord.xy + vec2(time * 31.0, time * 17.0)) - 0.5;
    color *= 0.995 + noise * 0.018;
    color *= vec3(1.035, 1.0, 0.945);
    color = vec3(1.0) - exp(-color * 1.30);
    color = pow(max(color, 0.0), vec3(1.0 / 2.2));

    float reflection = pow(
        max(0.0, 1.0 - length(glassPoint - vec2(-0.72, 0.78)) / 1.30),
        4.0
    );
    color += vec3(0.055, 0.070, 0.080) * reflection;
    color += vec3(0.004, 0.006, 0.009);

    gl_FragColor = vec4(color, 1.0);
}

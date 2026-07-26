precision mediump float;

uniform sampler2D frameTexture;

varying vec2 vTexCoord;

void main() {
    gl_FragColor = texture2D(frameTexture, vTexCoord);
}

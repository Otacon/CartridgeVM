#version 120

varying vec2 textureCoordinate;

void main() {
    gl_Position = gl_Vertex;
    textureCoordinate = gl_MultiTexCoord0.xy;
}

#version 330 core
out vec4 FragColor;

void main() {
    vec2 uv = (gl_FragCoord.xy / vec2(800, 600)) * 2.0 - 1.0;
    float dist = length(uv);

    // Draw white crosshair
    if (dist < 0.02 && (abs(uv.x) < 0.008 || abs(uv.y) < 0.008)) {
        FragColor = vec4(1.0);
    } else {
        discard;
    }
}
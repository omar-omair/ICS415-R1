Shader "Unlit/TruchetTunnel"
{
    Properties
    {
        _MainTex("Environment Map", 2D) = "white" {}
        _SurfaceTex("Surface Texture", 2D) = "white" {}
        _Speed("Animation Speed", Range(0.1, 2)) = 1
        _Iterations("Ray Steps", Int) = 64
        _Thickness("Thickness", Range(0.01, 0.5)) = 0.1
        _SuperQuadPower("Edge Sharpness", Range(1, 16)) = 8
        _Fisheye("Fisheye", Range(0, 1)) = 0.5
    }
    SubShader
    {
        Tags { "RenderType"="Opaque" }
        LOD 100
        
        Pass
        {
            CGPROGRAM
            #pragma vertex vert
            #pragma fragment frag
            #include "UnityCG.cginc"

            sampler2D _MainTex;
            sampler2D _SurfaceTex;
            float _Speed;
            int _Iterations;
            float _Thickness;
            float _SuperQuadPower;
            float _Fisheye;

            struct appdata
            {
                float4 vertex : POSITION;
                float2 uv : TEXCOORD0;
            };

            struct v2f
            {
                float2 uv : TEXCOORD0;
                float4 vertex : SV_POSITION;
                float3 ray : TEXCOORD1;
            };

            v2f vert (appdata v)
            {
                v2f o;
                o.vertex = UnityObjectToClipPos(v.vertex);
                o.uv = v.uv; // Use the original UV coordinates

                // Calculate full-screen coordinates for the ray direction
                float2 coord = (v.uv * 2.0 - 1.0) * float2(_ScreenParams.x/_ScreenParams.y, 1.0);
                #if UNITY_UV_STARTS_AT_TOP
                coord.y *= -1;
                #endif
                o.ray = normalize(float3(1.4 * coord, -1.0 + _Fisheye * dot(coord, coord)));
                return o;
            };

            float rand(float3 r) {
                return frac(sin(dot(r.xy, float2(1.38984*sin(r.z), 1.13233*cos(r.z)))) * 653758.5453);
            };

            float truchetarc(float3 pos)
            {
                float r = length(pos.xy);
                return pow(pow(abs(r - 0.5), _SuperQuadPower) +
                           pow(abs(pos.z - 0.5), _SuperQuadPower),
                           1.0/_SuperQuadPower) - _Thickness;
            };

            float truchetcell(float3 pos)
            {
                return min(min(
                    truchetarc(pos),
                    truchetarc(float3(pos.z, 1.0 - pos.x, pos.y))),
                    truchetarc(float3(1.0 - pos.y, 1.0 - pos.z, pos.x)));
            };

            float distfunc(float3 pos)
            {
                float3 cellpos = frac(pos);
                float3 gridpos = floor(pos);
                float rnd = rand(gridpos);

                if(rnd < 1.0/8.0) return truchetcell(float3(cellpos.x, cellpos.y, cellpos.z));
                else if(rnd < 2.0/8.0) return truchetcell(float3(cellpos.x, 1.0 - cellpos.y, cellpos.z));
                else if(rnd < 3.0/8.0) return truchetcell(float3(1.0 - cellpos.x, cellpos.y, cellpos.z));
                else if(rnd < 4.0/8.0) return truchetcell(float3(1.0 - cellpos.x, 1.0 - cellpos.y, cellpos.z));
                else if(rnd < 5.0/8.0) return truchetcell(float3(cellpos.y, cellpos.x, 1.0 - cellpos.z));
                else if(rnd < 6.0/8.0) return truchetcell(float3(cellpos.y, 1.0 - cellpos.x, 1.0 - cellpos.z));
                else if(rnd < 7.0/8.0) return truchetcell(float3(1.0 - cellpos.y, cellpos.x, 1.0 - cellpos.z));
                else return truchetcell(float3(1.0 - cellpos.y, 1.0 - cellpos.x, 1.0 - cellpos.z));
            };

            float3 gradient(float3 pos)
            {
                const float eps = 0.0001;
                float mid = distfunc(pos);
                return float3(
                    distfunc(pos + float3(eps, 0.0, 0.0)) - mid,
                    distfunc(pos + float3(0.0, eps, 0.0)) - mid,
                    distfunc(pos + float3(0.0, 0.0, eps)) - mid);
            };

            fixed4 frag(v2f i) : SV_Target
            {
                const float PI = 3.14159265359;
                float time = _Time.y * _Speed;

                // Camera rotation matrix
                float a = time / 3.0;
                float3x3 m = float3x3(
                    0.0, 1.0, 0.0,
                    -sin(a), 0.0, cos(a),
                    cos(a), 0.0, sin(a));
                // Squaring the matrix for a stronger rotation effect
                m = mul(m, m);
                m = mul(m, m);

                // Transform the ray direction with the camera rotation
                float3 ray_dir = mul(m, i.ray);
                float3 ray_pos = float3(
                    2.0 * (sin(time + sin(2.0 * time)/2.0 + 0.5)),
                    2.0 * (sin(time - sin(2.0 * time)/2.0 - PI/2.0)/2.0 + 0.5),
                    2.0 * ((-2.0 * (time - sin(4.0 * time)/4.0)/PI + 1.0)));

                // Ray marching loop
                int steps = _Iterations;
                for(int j = 0; j < _Iterations; j++)
                {
                    float dist = distfunc(ray_pos);
                    ray_pos += dist * ray_dir;
                    if(abs(dist) < 0.001) {
                        steps = j;
                        break;
                    }
                }

                // Lighting calculations
                float3 normal = normalize(gradient(ray_pos));
                float ao = 1.0 - (float)steps / _Iterations;
                float light = ao * pow(max(0, dot(normal, -ray_dir)), 2) * 1.4;

                // Compute the tunnel base color
                float3 tunnelColor = (cos(ray_pos/2.0) + 2.0)/3.0;
                tunnelColor *= light;

                float3 col = (cos(ray_pos / 2.0) + 2.0) / 3.0;
                
                // Environment reflection contribution (kept low)
                float3 reflected = reflect(ray_dir, normal);
                float3 env = tex2D(_SurfaceTex, reflected * reflected * reflected).xyz;
                
                // Final color: tunnel color modulated by lighting plus 10% of the texture highlight.
                float3 finalColor = col * light + 0.1 * env;
                
                // Final tunnel result
                float3 tunnelResult = tunnelColor + env;
                
                // Apply a vignette for a softer edge
                float vignette = pow(1.0 - length(i.uv * 2.0 - 1.0), 0.3);
                finalColor *= vignette;

                return fixed4(finalColor, 1.0);
            }
            ENDCG
        }
    }
}

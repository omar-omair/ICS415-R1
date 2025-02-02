from PIL import Image
import numpy as np

# Image dimensions
WIDTH, HEIGHT = 600, 600

# Define spheres and lights
spheres = [
    {"center": np.array([0, -1, 3]), "radius": 1, "color": (255, 0, 0), "shine": 500, "reflective": 0.2},
    {"center": np.array([2, 0, 4]), "radius": 1, "color": (0, 0, 255), "shine": 500, "reflective": 0.3},
    {"center": np.array([-2, 0, 4]), "radius": 1, "color": (0, 255, 0), "shine": 10, "reflective": 0.4},
    {"center": np.array([0, -5001, 0]), "radius": 5000, "color": (255, 255, 0), "shine": 1000, "reflective": 0.5},
]

lights = [
    {"type": "ambient", "intensity": 0.2},
    {"type": "point", "intensity": 0.6, "position": np.array([2, 1, 0])},
    {"type": "directional", "intensity": 0.2, "direction": np.array([1, 4, 4])}
]

# Camera parameters
viewport = {"width": 1, "height": 1, "distance": 1}


def canvas_to_viewport(x, y):
    return np.array([
        x * viewport["width"] / WIDTH,
        y * viewport["height"] / HEIGHT,
        viewport["distance"]
    ])


def ray_sphere_intersection(ray_origin, ray_direction, sphere):
    oc = ray_origin - sphere["center"]
    a = np.dot(ray_direction, ray_direction)
    b = 2 * np.dot(oc, ray_direction)
    c = np.dot(oc, oc) - sphere["radius"] ** 2
    discriminant = b ** 2 - 4 * a * c

    if discriminant < 0:
        return None, None
    t1 = (-b - np.sqrt(discriminant)) / (2 * a)
    t2 = (-b + np.sqrt(discriminant)) / (2 * a)
    return t1, t2


def closest_intersection(ray_origin, ray_direction, t_min, t_max):
    closest_t = float("inf")
    closest_sphere = None

    for sphere in spheres:
        t1, t2 = ray_sphere_intersection(ray_origin, ray_direction, sphere)

        if t1 is not None and t_min < t1 < t_max and t1 < closest_t:
            closest_t = t1
            closest_sphere = sphere
        if t2 is not None and t_min < t2 < t_max and t2 < closest_t:
            closest_t = t2
            closest_sphere = sphere

    return closest_sphere, closest_t


def compute_lighting(P, N, V, sphere):
    intensity = 0.0
    t_min = 0.001

    for light in lights:
        if light["type"] == "ambient":
            intensity += light["intensity"]
        else:
            if light["type"] == "point":
                L_unnormalized = light["position"] - P
                distance = np.linalg.norm(L_unnormalized)
                if distance == 0:
                    continue  # Avoid division by zero
                L = L_unnormalized / distance
                t_max = distance
            else:
                L = light["direction"]
                L = L / np.linalg.norm(L)
                t_max = float("inf")

            # Shadow check
            shadow_sphere, shadow_t = closest_intersection(P, L, t_min, t_max)
            if shadow_sphere is not None:
                continue

            # Diffuse
            n_dot_l = np.dot(N, L)
            if n_dot_l > 0:
                intensity += light["intensity"] * n_dot_l

            if sphere["shine"] > 0:
                R = 2 * N * np.dot(N, L) - L
                R = R / np.linalg.norm(R)
                V_normalized = V / np.linalg.norm(V)
                r_dot_v = np.dot(R, V_normalized)
                if r_dot_v > 0:
                    intensity += light["intensity"] * (r_dot_v ** sphere["shine"])

    return min(intensity, 1)


def trace_ray(ray_origin, ray_direction, depth=3):
    if depth <= 0:
        return (0, 0, 0)

    # Ensure ray_direction is normalized
    ray_direction = ray_direction / np.linalg.norm(ray_direction)

    closest_sphere, closest_t = closest_intersection(ray_origin, ray_direction, 1, float("inf"))
    if closest_sphere is None:
        return (0, 0, 0)

    P = ray_origin + closest_t * ray_direction
    N = P - closest_sphere["center"]
    N = N / np.linalg.norm(N)
    V = -ray_direction

    intensity = compute_lighting(P, N, V, closest_sphere)
    local_color = np.array(closest_sphere["color"]) * intensity

    # Reflections with offset
    if closest_sphere["reflective"] > 0:
        R = 2 * N * np.dot(N, V) - V
        R = R / np.linalg.norm(R)
        P_offset = P + N * 0.001
        reflected_color = trace_ray(P_offset, R, depth - 1)
        local_color = local_color * (1 - closest_sphere["reflective"]) + \
                     np.array(reflected_color) * closest_sphere["reflective"]

    return tuple(np.clip(local_color, 0, 255).astype(int))

def render_scene():
    image = Image.new("RGB", (WIDTH, HEIGHT))
    pixels = image.load()

    camera_origin = np.array([0, 0, 0])

    for x in range(WIDTH):
        for y in range(HEIGHT):
            px = (x - WIDTH / 2) * viewport["width"] / WIDTH
            py = -(y - HEIGHT / 2) * viewport["height"] / HEIGHT

            ray_direction = np.array([px, py, viewport["distance"]])
            ray_direction = ray_direction / np.linalg.norm(ray_direction)

            color = trace_ray(camera_origin, ray_direction)
            pixels[x, y] = color

    return image


def main():
    print("Rendering scene...")
    image = render_scene()
    image.save("output.png")
    print("Image saved as output.png")


main()
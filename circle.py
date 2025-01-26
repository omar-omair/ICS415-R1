from PIL import Image
import numpy as np

# Image dimensions
WIDTH, HEIGHT = 500, 500

# Define spheres and lights
spheres = [
    {"center": np.array([0, -1, 3]), "radius": 1, "color": (255, 0, 0), "shine": 500},  # Red
    {"center": np.array([2, 0, 4]), "radius": 1, "color": (0, 0, 255), "shine": 500},  # Blue
    {"center": np.array([-2, 0, 4]), "radius": 1, "color": (0, 255, 0), "shine": 10},  # Green
    {"center": np.array([0, -5001, 0]), "radius": 5000, "color": (255, 255, 0), "shine": 1000},  # Yellow big
]

lights = [
    {"type": "ambient", "intensity": 0.2},
    {"type": "point", "intensity": 0.6, "position": np.array([2, 1, 0])},
    {"type": "directional", "intensity": 0.2, "direction": np.array([1, 4, 4])}
]

# Camera parameters
viewport = {"width": 1, "height": 1, "distance": 1}
viewport_size = viewport["width"] * viewport["height"]

t_min = 1
t_max = float("inf")

def canvas_to_viewport(x, y):
    return np.array([
        x * viewport_size / WIDTH, 
        y * viewport_size / HEIGHT, 
        viewport["distance"]
    ])


# Helper function: Ray-sphere intersection
def ray_sphere_intersection(ray_origin, ray_direction, sphere):
    oc = ray_origin - np.array(sphere["center"])
    a = np.dot(ray_direction, ray_direction)
    b = 2 * np.dot(oc, ray_direction)
    c = np.dot(oc, oc) - sphere["radius"] ** 2
    discriminant = b ** 2 - 4 * a * c

    if discriminant < 0:
        return (None, None)  # No intersection
    t1 = (-b - np.sqrt(discriminant)) / (2 * a)
    t2 = (-b + np.sqrt(discriminant)) / (2 * a)
    return (t1, t2)

# Helper function: Compute pixel color
def trace_ray(ray_origin, ray_direction):
    closest_t = float("inf")
    closest_sphere = None
    pixel_color = (255,255,255)  # Background color (white)

    for sphere in spheres:
        t1, t2 = ray_sphere_intersection(ray_origin, ray_direction, sphere)

        if t1 != None and t_min < t1 < t_max and t1 < closest_t:
            closest_t = t1
            closest_sphere = sphere
        if t2 != None and t_min < t2 < t_max and t2 < closest_t:
            closest_t = t2
            closest_sphere = sphere


    if closest_sphere is None: 
        return pixel_color

    P = ray_origin + closest_t * ray_direction
    N = P - closest_sphere["center"]
    N = N / np.linalg.norm(N)
    view_direction = -ray_direction
    intensity = computeLighting(P, N, view_direction, closest_sphere)
    color = np.array(closest_sphere["color"]) * intensity

    pixel_color = tuple(color.astype(int))

    return pixel_color

# Compute the lights
def computeLighting(P, N, V, sphere):
    intensity = 0.0
    for l in lights:
        if l["type"] == "ambient":
            intensity += l["intensity"]
        else:
            if l["type"] == "point":
                light_vector = l["position"] - P
            else:
                light_vector = l["direction"]
            
            light_vector = light_vector / np.linalg.norm(light_vector)

            n_dot_l = np.dot(N, light_vector)

            if n_dot_l > 0:
                intensity += l["intensity"] * n_dot_l/(np.linalg.norm(N) * np.linalg.norm(P))
            
            if sphere["shine"] != -1:
                R = 2 * N * np.dot(N, light_vector) - light_vector
                r_dot_v = np.dot(R, V)
                if r_dot_v > 0:
                    intensity += l["intensity"] * (r_dot_v / (np.linalg.norm(R) * np.linalg.norm(V))) ** sphere["shine"]

    return min(intensity, 1)

# Render the scene
def render_scene():
    # Create an empty image
    image = Image.new("RGB", (WIDTH, HEIGHT))
    pixels = image.load()

    # Camera origin
    camera_origin = np.array([0.0, 0.0, 0.0])

    for x in range(WIDTH):
        for y in range(HEIGHT):
            # Map pixel to viewport
            px = (x - WIDTH / 2)
            py = -(y - HEIGHT / 2)

            # Ray direction
            ray_direction = canvas_to_viewport(px,py)
            ray_direction = ray_direction / np.linalg.norm(ray_direction)  # Normalize the ray direction

            # Compute pixel color
            color = trace_ray(camera_origin, ray_direction)
            pixels[x, y] = color

    return image

# Main function
def main():
    print("Rendering scene...")
    image = render_scene()
    image.save("output.png")
    print("Image saved as output.png")

main()

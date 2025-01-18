from PIL import Image
import numpy as np

# Image dimensions
WIDTH, HEIGHT = 500, 500

# Define spheres
spheres = [
    {"center": (0,-1, 3), "radius": 1, "color": (255, 0, 0)},  # Red
    {"center": (2, 0, 4), "radius": 1, "color": (0, 255, 0)},    # Green
    {"center": (-2, 0, 4), "radius": 1, "color": (0, 0, 255)},  # Blue
]

# Camera parameters
viewport = {"width": 1, "height": 1, "distance": 1}

t_min = 1
t_max = float("inf")

# Helper function: Ray-sphere intersection
def ray_sphere_intersection(ray_origin, ray_direction, sphere):
    oc = ray_origin - np.array(sphere["center"])
    a = np.dot(ray_direction, ray_direction)
    b = 2 * np.dot(oc, ray_direction)
    c = np.dot(oc, oc) - sphere["radius"] ** 2
    discriminant = b ** 2 - 4 * a * c

    if discriminant < 0:
        return (None,None)  # No intersection
    t1 = (-b - np.sqrt(discriminant)) / (2 * a)
    t2 = (-b + np.sqrt(discriminant)) / (2 * a)
    return (t1,t2)

# Helper function: Compute pixel color
def compute_color(ray_origin, ray_direction):
    closest_t = float("inf")
    pixel_color = (255, 255, 255)  # Background color (white)

    for sphere in spheres:
        t1,t2 = ray_sphere_intersection(ray_origin, ray_direction, sphere)

        for t in (t1, t2):
            if t != None and t_min <= t and t < closest_t: 
                closest_t = t
                pixel_color = sphere["color"]

    return pixel_color

# Render the scene
def render_scene():
    # Create an empty image
    image = Image.new("RGB", (WIDTH, HEIGHT))
    pixels = image.load()

    # Camera origin
    camera_origin = np.array([0, 0, 0])

    for y in range(HEIGHT):
        for x in range(WIDTH):
            # Map pixel to viewport
            px = (x / WIDTH) * viewport["width"] - (viewport["width"] / 2)
            py = -(y / HEIGHT) * viewport["height"] + (viewport["height"] / 2)
            pz = viewport["distance"]

            # Ray direction
            ray_direction = np.array([px, py, pz])

            # Compute pixel color
            color = compute_color(camera_origin, ray_direction)
            pixels[x, y] = color

    return image

# Main function
def main():
    print("Rendering scene...")
    image = render_scene()
    image.save("output.png")
    print("Image saved as output.png")

main()
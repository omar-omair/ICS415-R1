using UnityEngine;

[ExecuteInEditMode]
public class FullScreenEffect : MonoBehaviour
{
    public Material effectMaterial;

    private void OnRenderImage(RenderTexture source, RenderTexture destination)
    {
        if (effectMaterial != null)
        {
            effectMaterial.SetFloat("_TimeY", Time.time);
            effectMaterial.SetVector("_Resolution", new Vector2(Screen.width, Screen.height));
            Graphics.Blit(source, destination, effectMaterial);
        }
        else
        {
            Graphics.Blit(source, destination);
        }
    }
}

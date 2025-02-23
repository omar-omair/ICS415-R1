using UnityEngine;

public class PlasmaShaderController : MonoBehaviour
{
    public Material plasmaMaterial;

    void Update()
    {
        if (plasmaMaterial)
        {
            // Update _TimeY to make it change over time
            plasmaMaterial.SetFloat("_TimeY", Time.time);
            
            // Update resolution based on the screen size
            plasmaMaterial.SetVector("_Resolution", new Vector2(Screen.width, Screen.height));
        }
    }
}
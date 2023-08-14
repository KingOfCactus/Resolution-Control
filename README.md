<p align="center">
	<img width=256px src="GitHub/logo.png" />
</p>

# ResolutionControl+ Forge
A fork of [LuZWCHA's ResolutionControl+ Forge](https://github.com/LuZWCHA/Resolution-Control), that is a forge fork of [Resolution Control+](https://github.com/UltimateBoomer/Resolution-Control) which is originally a fork of [Resolution Control](https://github.com/juliand665/Resolution-Control)
with new functionality. Yeah, lots of forks.

This is a port to 1.17.2 and 1.18.2 that i made some months ago without previous forge modding knowledge. The option menu background is buggy, linear scale is broken, dynamic resolution is broken, and there's probably other broken things i don't remember now. Shaders seems to work tho. With that said, **use this at your own risk 👍**.

![main](GitHub/lowresshaders.png)

# Original Description Below

## Features

### Resolution Scaling

ResolutionControl+ allows you to change Minecraft's render resolution separately from the HUD elements.
If you have a good GPU, you can increase the multiplier for anti-aliasing.
Otherwise, you can lower the multiplier to improve performance or give Minecraft a retro style.
You can also set a custom multiplier value by pressing the `S` button.

Additionally, you can set the upscale/downscale algorithm used to scale the render to the viewport.
Linear is useful as an anti-aliasing filter,
and nearest neighbor generally looks better for lower than native resolutions.

![main](GitHub/mainsettings.png)

---

### Screenshots

ResolutionControl+ Forge can be used to take larger than native resolution screenshots.
To do this, use the `Screenshot Framebuffer` keybind which is unbound by default.

## Settings

**Resolution Scaling**

- Render scale
    - 0.0 - 8.0x (can be exceeded by manually setting the value)
    - An estimate VRAM usage is displayed
- Upscale/downscale algorithm  
    - Linear, nearest

**Screenshots**

- Use set size
    - Enabled: render screenshots at the specified resolution
        - Note: can produce artifacts if the screenshot aspect ratio is significantly different 
          from the aspect ratio of the viewport
    - Disabled: render at the scaled render resolution
- Always allocated
    - Enabled: screenshot framebuffer is always allocated in memory
        - May reduce screenshot pause times, at the cost of more VRAM usage during normal gameplay
    - Disabled: screenshot framebuffer is allocated on-demand and freed immediately after
- Screenshot size
    - Screenshot size if `use set size` is enabled
    - An estimate VRAM usage is displayed
    
---

## Compatibility

Currently, ResolutionControl+ **does not work with Fabulous Graphics**.

**Optifine** - Compatible, stacks with its own render scale implementation



# ![BadApplePlugin](https://img.shields.io/badge/BadApplePlugin-v1.0.0-blue?style=for-the-badge)  

![Java](https://img.shields.io/badge/Java-8%20to%2021-green?style=flat-square)  
![Spigot](https://img.shields.io/badge/Spigot-1.8%20to%201.21.8-red?style=flat-square)  


---

## 📌 Overview
**BadApplePlugin** is a Spigot/Bukkit plugin that plays the iconic **Bad Apple** animation using ASCII holograms and NoteBlockAPI music. It supports Minecraft versions **1.8 through 1.21.8** and allows players to experience the animation in a dedicated theater world.

- Displays ASCII animation via invisible armor stands
- Plays `Bad Apple` music using [NoteBlockAPI](https://www.spigotmc.org/resources/noteblockapi.19287/)
- Fully configurable frames and music
- Automatic player preparation for theater view
- Compatible with single-player and multi-player servers

---

## ⚙️ Features

- **Dynamic Theater World**: Creates a world for the animation, with barriers as a stage.  
- **ASCII Holograms**: Renders the frames line by line using invisible armor stands.  
- **Music Integration**: Plays `.nbs` files using NoteBlockAPI.  
- **Player Control**: Prevents movement and chat during the animation.    

---

## 📥 Installation

1. **Download Plugin**

   Download the latest version from:
   - [Spigot](https://www.spigotmc.org/resources/badapple-%E2%80%93-ascii-animation-music-plugin-for-minecraft.128284/)
   - Or via **GitHub Releases**: `https://github.com/OtakuSweett/BadApple-Spigot/releases`  

2. **Place Plugin in Server**

   Move `BadApplePlugin.jar` into your server's `/plugins` folder.

3. **Download Frames**

   The frames file is required for animation:

    [https://cdn.devsweett.com/badapple/frames.txt](https://cdn.devsweett.com/badapple/frames.txt)



Place the file in:

```

/plugins/BadApple/badapple_frames.txt

````

4. **Add NoteBlockAPI**

Download `NoteBlockAPI.jar` and place it in your server's `/plugins` folder.  

5. **Start Server**

Start your Spigot/Bukkit server. The plugin will automatically create its theater world and load the frames/music.

---

## ⚙️ Configuration

- **Frames File**: `plugins/BadApple/badapple_frames.txt`  
- Place your custom ASCII frames separated by `---FRAME---`.
- **Music File**: `plugins/BadApple/Bad Apple.nbs`  
- Default included, or replace with your own `.nbs` file.  

---

## 🚀 Usage

- Players join the server and are automatically teleported to the theater.  
- Animation starts automatically after 2 seconds (configurable in code if needed).  
- Players are returned to their original location after the animation ends.  
- Movement, flying, and chat are temporarily disabled during playback.

---

## 🛠️ Supported Versions

- Minecraft: **1.8 → 1.21.8**  
- Java: **1.8 → 21**  
- Spigot/Bukkit API: Compatible with official builds  

---

## 🔧 Development

Clone the repository and build with Maven:

```bash
git clone https://github.com/OtakuSweett/BadApple-Spigot
cd BadApplePlugin
mvn install:install-file -Dfile=NoteBlockAPI.jar -DgroupId=com.xxmicloxx -DartifactId=NoteBlockAPI -Dversion=1.5.0 -Dpackaging=jar
mvn clean package
````

Make sure `NoteBlockAPI.jar` is in your project root to compile successfully.

---

## 📂 File Structure

```
BadApplePlugin/
├─ src/main/java/com/otakusweett/badapple/spigot
├─ src/main/resources
├─ plugins/BadApple/badapple_frames.txt
└─ BadApple.nbs
```


---

## 🌐 Links

* **Live Server Demo:** 
* badapple.devsweett.com
* MC Bedrock:
* badapple.devsweett.com
* 44440
* **Frames CDN:** [cdn.devsweett.com/badapple/frames.txt](https://cdn.devsweett.com/badapple/frames.txt)
* **NoteBlockAPI:** [SpigotMC Resource](https://www.spigotmc.org/resources/noteblockapi.2155/)

---

![Footer](https://img.shields.io/badge/BadApple-Enjoy%20the%20Animation-ff69b4?style=for-the-badge)


<img width="794" height="508" alt="{0DF77DD6-AD0A-4A52-82C4-0C927BCB9ED6}" src="https://cdn.devsweett.com/github/img/BadApplePlugin.png" />



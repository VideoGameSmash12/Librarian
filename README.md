# Librarian
Librarian is a mod for Fabric that improves the saved hotbar system by adding new features and increasing the overall
capacity with a page system. It is the successor to Hotbars+, a mod by the same author.

## Features

### Accidental overwrite protection
A common problem people experience with the saved hotbar system is the potential to unintentionally overwrite existing
hotbars by pressing the wrong key, losing many items by complete accident in the process. Librarian tries to help 
prevent this from happening by comparing the date of the items being saved to that of the items already saved. If it
doesn't exactly match, a prompt will come up asking for confirmation before continuing, potentially saving you and your
items.

### Infinite saved hotbar capacity
Never run out of space in your hotbars again. Librarian introduces a page system which allows you to store as many items
as you could possibly ever want or need.

### Renamable saved hotbars
To help facilitate distribution and organization, Librarian introduces a new vanilla-compatible hotbar metadata system
which allows you to rename your saved hotbars to literally anything you want along with other bits of metadata like a
description and even a list of authors. Names and descriptions support both chat color codes and also
[MiniMessage](https://docs.advntr.dev/minimessage/format.html).

All metadata can be customized in-game with the client-side command `/librarian meta`*, but users can also rename their
hotbar page by simply clicking the text in the creative inventory menu.

\* - Some versions (such as pre-1.14.4) do not currently have client-side command libraries available, so with these
versions you will have to apply the changes manually with an NBT editor. I personally recommend using NBTExplorer for
this purpose.

### Multi-version support
To account for those holding out on specifics game versions, Librarian supports a wide range of Minecraft versions as
recent as 1.21.5 and as old as 1.12.2 (with the help of [Legacy Fabric](https://legacyfabric.net/) and
[Ornithe](https://ornithemc.net/)). I am committed to supporting as many versions as reasonably possible. The foundation
of Librarian, while similar to its predecessor, is written with the goal of not relying on any particular
version-specific code. This helps make it more future-proof in the long run.

Currently supported versions:
* 1.12.2 (Legacy Fabric or Ornithe)
* 1.13.2 (Legacy Fabric or Ornithe)
* 1.14.4 (Regular Fabric or Ornithe)
* 1.15.2
* 1.16.5
* 1.17.1
* 1.18.2
* 1.19
* 1.19.2
* 1.19.4
* 1.20.1
* 1.20.2
* 1.20.4
* 1.20.6
* 1.21.1
* 1.21.3
* 1.21.4
* 1.21.5

## Building
Librarian requires at least JDK 21 to be compiled. You can compile it by running `gradlew build` (or, if you are running
Windows, `./gradlew.bat build`). The resulting JAR will be located in `/build/libs`.
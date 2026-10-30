# A Blueprint lives in world data, named by its sheet

A **Blueprint** is captured from a **Selection** of up to 32,768 positions by default, and a **Cut** carries everything its blocks hold, a chest's contents among them. Groundworks keeps everything else it remembers on the held stack (a **Stretch**'s start, a turn, a height, a **Dismantle**'s start), but a stack's components are sent to the client in every inventory and container packet and written into every container the stack sits in. A Blueprint there would be a payload of tens of kilobytes or more, sent again and again. Building Gadgets 2 keeps its copies and cuts in server saved data keyed by an id on the gadget, for the same reason.

**Decision.** A Blueprint's blocks live in the server's world data, shared by every dimension, under an id that the sheet holds and nothing else does.

- **A Copy's last ten pastes are kept with it**, each with its dimension and what it laid, for **Undo Paste**, and go when the Copy is cleared or pruned.
- **The client is sent a Blueprint only while a sheet naming it is held**, to draw its **Placement Preview**.
- **Pasting a Cut consumes its data**, and a sneak-use in the air clears a **Copy**'s; either leaves the sheet blank. Two sheets naming one Cut can never both paste it: the second finds it gone.
- **Each entry records its owner and when it was last held or pasted.** A server config, 30 days by default, prunes Copies unused for that long, since nothing tells the server when a sheet is burnt or binned. Cuts are never pruned: they are the only place their blocks still exist.
- **An operator command lists the Cuts**, with owner, size and age, and gives a sheet naming one, so blocks on a lost sheet can be recovered.
- **A Cut's sheet survives fire and lava and never despawns**, so a Cut is lost only to the void or to deliberate destruction, and then only until an operator recovers it.

## Considered Options

- **The Blueprint on the sheet**, as every other stored value is. Rejected: the payload, sent with every inventory update and stored in every chest, at the cap the server allows.
- **A Copy on the sheet and a Cut in world data.** Rejected: a Copy at the cap is as large as a Cut without its contents, and two stores make two lifetimes to reason about.
- **Drop data whose sheet can't be found.** Rejected: a sheet in an unloaded chunk or an offline player's inventory can't be found either, and a Cut dropped that way destroys blocks.

## Consequences

- The world gains Groundworks' first saved data; removing the mod from a world loses every Cut not yet pasted.
- A Blueprint's preview needs a request to the server and a wait before the first frame, unlike every other plan, which is drawn from what the client already has.
- Sheets copied by a creative player's middle-click name the same Blueprint, which is harmless for a Copy and, for a Cut, still pastes once.

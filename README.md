# betterquicksell

a client-side fabric mod for **donutsmp** that clicks **yes** on quick-sell confirmation dialogs for you, but only for items you've whitelisted.

when donutsmp opens a dialog that says *"are you sure you want to sell this?"*, betterquicksell checks the item in your main hand. if that item is on your whitelist and the stack isn't bigger than the size you allowed, the mod presses **yes**. otherwise the dialog stays open and you decide yourself.

## features

- **auto-confirm:** detects the sell confirmation dialog and presses its **yes** button.
    - make sure `Auction Quick Sell` is disabled in `/settings`
- **whitelist:** only items you've added get auto-confirmed. each entry has a maximum stack size, so you can allow selling 16 diamonds without allowing 64.
- **in-game editor:** add and remove entries from a screen in the game. **use held item** fills in the item id and stack size from your main hand.
- **toggle:** turn auto-confirm on or off with a keybind. the current state shows above your hotbar.

## keybinds

| key | action |
| --- | --- |
| `j` | toggle quick sell auto-confirm |
| `k` | open the whitelist editor |

you can rebind both under **options → controls → key binds → better quick sell**.

## whitelist

open the editor with `k`, then:

1. enter an item id such as `minecraft:diamond`, or click **use held item**.
2. enter a maximum stack size from 1 to 99. if you leave it blank, it defaults to 64.
3. click **add**.

an item matches an entry when its id is the same and the held stack is no larger than the entry's stack size. if the id isn't valid, the field turns red and **add** stays disabled.

the whitelist is saved to `config/betterquicksell.json`:

```json
[
  {
    "item": "minecraft:diamond",
    "count": 64
  }
]
```

when the file loads, entries with unknown item ids or a count below 1 are skipped.
Version 1.0.2
-------------

\* Fixed NPEs when a player hasn't had any block counts saved (Fixes [#1](https://github.com/AuraDevelopmentTeam/AuraBlockLimit/issues/1)).  
\* Fixed potential issue with inherit loops in translation files.  
\* Improved placeholder performance.  


Version 1.0.1
-------------

\+ Allow customization of the pagination headers and footers through the language file.  


Version 1.0.0
-------------

\+ Added the `/limit` command to show the player their current limit.  
\+ Added the `/limit reload` command to reload the config.  
\+ Added a message when the block limit is hit.  
\+ Added translation system.  
\+ Added option to completely ignore blocks (by setting their limit to `-2`).  
\+ Added config option to control the fallback value of blocks (either `-1` or `-2`).  


Version 0.1.0
-------------

\+ Added h2 data storage.  
\+ Added MySQL data storage.  
\+ Implemented block counting
\+ Prevent placing blocks if limit (set through metas) is met.  


Version 0.0.0
-------------

\* Initial commit  

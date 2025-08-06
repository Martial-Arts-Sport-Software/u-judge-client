## Short Info
<b>U'Judge - Client</b> is mobile application for Android & IOS, designed for judging sport competitions
- Based on Kotlin Multiplatform technology
- Part of U'Judge Platform, client & server system for judging Taekwondo & Hapkido sport competition
- The appearance of the application is designed in accordance with our <a href="https://www.figma.com/design/x5vY9DbXh3a0kv0lBPcNru/Judging-app?node-id=51-128&t=r4o8EgEnSLslF1dG-0">specific design</a>

## Features
Application has two main modes - online and offline
Note: both modes requires username filled to work

### Offline Mode (No server required)
- Does not need server, but some of disciplines are locked to judge (kerugi & tanbon, they require server to display information about current bout)
- From all dicipline screens there is settings button, that toggles settings menu. Menu differs depending on current discipline.
- Support two languages, russian & english, korean language also is planning to be added. Language select is available on entry screen and in settings for every discipline
- Technical disciplines (hosinsool & freestyle) are available, but "Send" button is disabled, user can only save his judging ratings
- In technical disciplines there is info button, that shows information in seleteced language to remind competition rules

### Online Mode (Server required)
- This mode has all disciplines available, but requires server connection.
- To use online mode, user must enter server's IP address, that can be got from server application.
- In combat disciplines (kerugi & tanbon), user can click on buttons that represent different hits by competitors.
- In case if something is wrong, there is "Attention button", that send signal to server, and bout is stopped.
- All offline mode features are also available

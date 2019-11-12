package bot;

public enum MenuState {
    MAIN_MENU("Main Menu"),
    TOOLS_MENU("Tools Menu"),
    PHOTO_GETTER("Photo Getter"),
    GAMES_MENU("Games Menu"),
    CITIES_GAME("Cities Game"),
    LOCATOR("Locator");

    private String name;

    MenuState(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }
}

package bot.model;

import bot.games.cities.CitiesGame;
import bot.tools.PhotoGetter;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class Model {

    private MenuState menuState;
    private CitiesGame citiesGame;
    private static final Logger logger = Logger.getLogger(Model.class.getName());
    private HashMap<MenuState, StateData> statesInfo = new HashMap<>();
    //private boolean keyboardEnabled = true;

    public Model(){
        statesInfo.put(MenuState.MAIN_MENU, new StateData(MenuState.MAIN_MENU.getName(), "Здесь можно выбрать нужную категорию",
                new ArrayList<>(Arrays.asList(MenuState.TOOLS_MENU, MenuState.GAMES_MENU)), null));
        statesInfo.put(MenuState.TOOLS_MENU, new StateData(MenuState.TOOLS_MENU.getName(), "Здесь можно воспользоваться разными сервисами",
                new ArrayList<>(Arrays.asList(MenuState.PHOTO_GETTER, MenuState.LOCATOR)), MenuState.MAIN_MENU));
        statesInfo.put(MenuState.GAMES_MENU, new StateData(MenuState.GAMES_MENU.getName(), "Здесть можно выбрать игру",
                new ArrayList<>(Arrays.asList(MenuState.CITIES_GAME)), MenuState.MAIN_MENU));
        statesInfo.put(MenuState.PHOTO_GETTER, new StateData(MenuState.PHOTO_GETTER.getName(), "Скажи, что должно быть на картинке, и я поищу что-нибудь подобное",
                null, MenuState.TOOLS_MENU));
        statesInfo.put(MenuState.CITIES_GAME, new StateData(MenuState.CITIES_GAME.getName(), "Назови город, и начнем", null, MenuState.GAMES_MENU));
        statesInfo.put(MenuState.LOCATOR, new StateData(MenuState.LOCATOR.getName(), "Это меню Локатора", null, MenuState.TOOLS_MENU));
//        setupInlineKeyboards();
    }

    public MenuState getMenuState(){
        return menuState;
    }

    public HashMap<MenuState, StateData> getStatesInfo(){
        return statesInfo;
    }

    public void updateMenuState(String message){
        toMainMenu(message);
        toBackMenu(message);
        StateData stateData = statesInfo.get(menuState);
        if (stateData.getChilds() != null) {
            List<MenuState> childs = stateData.getChilds();
            for (MenuState child : childs) {
                if (child.getName().equals(message))
                    menuState = child;
            }
        }
    }


    public String getStateAnswer(String message) throws IOException {
        switch (menuState){
            case CITIES_GAME:
                if (message.equals(MenuState.CITIES_GAME.getName())) {
                    citiesGame = new CitiesGame();
                    break;
                }
                return citiesGame.getAnswer(message);

            case PHOTO_GETTER:
                if (message.equals(MenuState.PHOTO_GETTER.getName())) break;
                return PhotoGetter.getPhotoURL(message);

           /*Шрек case MAIN_MENU:
                if (message.equals("Shrek"))
                    return new File(System.getProperty("user.dir") +
                            File.separator + "src" + File.separator + "main" +
                            File.separator + "resources" + File.separator + "Shrek.gif").getAbsolutePath();
                break;*/
        }
        return "";
    }

    public String getStateInfoText(){
        return statesInfo.get(menuState).getInfoText();
    }

    private void toMainMenu(String message){
        if(message.equals(MenuState.MAIN_MENU.getName()) || message.equals("/start"))
            menuState = MenuState.MAIN_MENU;
    }

    private void toBackMenu(String message){
        MenuState parent = statesInfo.get(menuState).getParent();
        if(parent != null && (message.equals(parent.getName()) || message.equals("Back")))
            menuState = parent;
    }

/*    private void setupInlineKeyboards(){
        for (Map.Entry<MenuState, StateData> entry : statesInfo.entrySet()){
            StateData data = entry.getValue();
            List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
            List<InlineKeyboardButton> buttonsRow1 = new ArrayList<>();
            List<InlineKeyboardButton> buttonsRow2 = new ArrayList<>();
            if (data.getChildren() != null) {
                List<MenuState> childs = data.getChilds();
                for (MenuState child : children) {
                    String childName = statesInfo.get(child).getName();
                    InlineKeyboardButton button = new InlineKeyboardButton().setText(childName).setCallbackData(childName);
                    buttonsRow1.add(button);
                }
            }
            if (data.getParent() != null) {
                String parentName = statesInfo.get(data.getParent()).getName();
                buttonsRow2.add(new InlineKeyboardButton().setText("< Back").setCallbackData(parentName));
                buttonsRow2.add(new InlineKeyboardButton().setText("Main").setCallbackData(MenuState.MAIN_MENU.getName()));
            }
            buttons.add(buttonsRow1);
            buttons.add(buttonsRow2);
            InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
            markupKeyboard.setKeyboard(buttons);
            data.keyboard = new InlineKeyboardMarkup();
            data.keyboard.setKeyboard(buttons);
        }
    }

    public InlineKeyboardMarkup getKeyboard(){
        return statesInfo.get(menuState).keyboard;
    }

    public boolean isKeyboardEnabled() {
        return keyboardEnabled;
    }

    public void switchKeyboard(){
        keyboardEnabled = keyboardEnabled ? false : true;
    }*/
}
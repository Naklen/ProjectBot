package bot.telegram;

import bot.BotProperties;
import bot.games.cities.GameState;
import bot.model.MenuState;
import bot.model.Model;
import bot.model.StateData;
import bot.tools.locator.Location;
import com.google.inject.internal.cglib.core.$WeakCacheKey;
import org.json.JSONObject;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiValidationException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public class TelegramBot extends TelegramLongPollingBot {

    public TelegramBot() {
        setupInlineKeyboards();
        statesInfo.get(MenuState.LOCATOR).keyboard = getLocationKeyboard();
        statesInfo.get(MenuState.MOVIE_RANDOMIZER).keyboard = getMovieRandomizerKeyboard();
    }

    private static final Logger logger = Logger.getLogger(TelegramBot.class.getName());
    private Model model = new Model();
    private HashMap<MenuState, StateData> statesInfo = Model.statesInfo;
    private HashMap<Long, Model> chatIdModel = new HashMap<>();
    private boolean needToDeleteKeyboard = false;

    public static void main(String[] args) {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try{
            TelegramBot bot = new TelegramBot();
            telegramBotsApi.registerBot(bot);
        } catch (TelegramApiException e){
            logger.info(e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery() || update.hasMessage()){
            Long chatId = null;
            Message message = null;
            String data = null;
            BiConsumer<Message, String> deliveryman = null;
            if(update.hasCallbackQuery()){
                chatId = update.getCallbackQuery().getMessage().getChatId();
                message = update.getCallbackQuery().getMessage();
                data = update.getCallbackQuery().getData();
                deliveryman = (m, t) -> editMessageText(m, t);
                if (data.equals(MenuState.LOCATOR.getName()) || data.equals(MenuState.MOVIE_RANDOMIZER.getName()))
                    deliveryman = (m, t) -> sendMessage(m, t);
            }
            if(update.hasMessage()){
                chatId = update.getMessage().getChatId();
                message = update.getMessage();
                data = message.getText();
                deliveryman = (m, t) -> sendMessage(m, t);
            }
            if (message.hasLocation()){
                logger.info("Location received");
                org.telegram.telegrambots.meta.api.objects.Location location = message.getLocation();
                String locationUpdatedAnswer = model.locator.updateLocation(new Location(location.getLatitude(), location.getLongitude()));
                deliveryman.accept(message, locationUpdatedAnswer);
            }
            logger.info("chat id " + chatId);
            if(!chatIdModel.containsKey(chatId))
                chatIdModel.put(chatId, new Model());
            model = chatIdModel.get(chatId);
            if (data != null && !data.isEmpty()){
                MenuState lastState = model.getMenuState();
                model.updateMenuState(data);
                if(lastState != model.getMenuState()) {
                    String infoText = model.getStateInfoText();
                    if (lastState != null && model.isStateWithReplyKeyboard(lastState)){
                        needToDeleteKeyboard = true;
                        sendMessageWithDeletingReplyKeyboard(message, infoText);
                    }
                    else {
                        needToDeleteKeyboard = false;
                        deliveryman.accept(message, infoText);
                    }
                }
                String answer;
                try {
                    answer = model.getStateAnswer(data);
                }
                catch (Exception e){
                    answer = null;
                    switch (model.getMenuState()) {
                        case PHOTO_GETTER:
                            deliveryman.accept(message, "Image not found");
                            break;
                        default:
                            logger.info(e.getMessage());
                            deliveryman.accept(message, "Error");
                    }
                }
                if(answer != null && !answer.isEmpty()) {
                    switch (model.getMenuState()) {
/*Шрек                        case MAIN_MENU:
                            logger.info("sendAnim");
                            sendAnimationFromDisk(message, answer);
                            break;*/

                        case PHOTO_GETTER:
                            sendPhotoByURL(message, answer);
                            break;
                        case MOVIE_RANDOMIZER:
                        case LOCATOR:
                            if (answer.contains("{")) {
                                JSONObject jsonAnswer = new JSONObject(answer);
                                String messageText = jsonAnswer.getString("message");
                                String url = jsonAnswer.getString("url");
                                sendMessage(message, messageText);
                                sendPhotoByURL(message, url);
                            }
                            else
                                sendMessage(message, answer);
                            break;
                        default:
                            logger.info("default");
                            deliveryman.accept(message, answer);
                    }
                }
                logger.info(model.getMenuState().toString());
            }
        }
    }

    private void sendMessage(Message message, String text){
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(text);
        ReplyKeyboard keyboard = getKeyboard();
        if (keyboard != null && (model.getMenuState() != MenuState.LOCATOR ||
                (model.locator != null && !model.locator.isLocationInitiallyUpdated())))
            sendMessage.setReplyMarkup(keyboard);
        try {
            execute(sendMessage);
        }catch (TelegramApiException e){
            logger.info(e.getMessage());
        }
    }

    private void editMessageText(Message message, String text){
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.enableMarkdown(true);
        editMessageText.setChatId(message.getChatId());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.setText(text);
        ReplyKeyboard keyboard = getKeyboard();
        if (keyboard != null)
            editMessageText.setReplyMarkup((InlineKeyboardMarkup) keyboard);
        try {
            execute(editMessageText);
        }catch(TelegramApiException e){
            logger.info(e.getLocalizedMessage());
        }
    }

    private void sendMessageWithDeletingReplyKeyboard(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(text);
        sendMessage.setReplyMarkup(new ReplyKeyboardRemove());
        Message newMessage = null;
        try {
             newMessage = execute(sendMessage);
        }catch (TelegramApiException e){
            logger.info(e.getMessage());
        }
        deleteMessage(newMessage);
        sendMessage(newMessage, text);
    }

    private void deleteMessage(Message message) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(message.getChatId());
        deleteMessage.setMessageId(message.getMessageId());
        try{
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            logger.info(e.getMessage());
        }
    }

    private void sendPhotoByURL(Message message, String url){
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(url);
        sendPhoto.setChatId(message.getChatId().toString());
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            logger.info(e.getMessage());
        }
    }


/*Шрек    private void sendAnimationFromDisk(Message message, String path){
        SendAnimation sendAnimation = new SendAnimation();
        sendAnimation.setAnimation(new File(path));
        sendAnimation(message, sendAnimation);
    }

    private void sendAnimation(Message message, SendAnimation sendAnimation){
        sendAnimation.setChatId(message.getChatId().toString());
        try {
            execute(sendAnimation);
        } catch (TelegramApiException e) {
            logger.info(e.getMessage());
        }
    }*/

    private void setupInlineKeyboards(){
        for (Map.Entry<MenuState, StateData> entry : statesInfo.entrySet()){
            StateData data = entry.getValue();
            List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
            List<InlineKeyboardButton> buttonsRow1 = new ArrayList<>();
            List<InlineKeyboardButton> buttonsRow2 = new ArrayList<>();
            if (data.getSubmenus() != null) {
                List<MenuState> submenus = data.getSubmenus();
                for (MenuState submenu : submenus) {
                    String childName = statesInfo.get(submenu).getName();
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
            ((InlineKeyboardMarkup) data.keyboard).setKeyboard(buttons);
        }
    }

    private ReplyKeyboardMarkup getLocationKeyboard(){
        List<KeyboardRow> buttons = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        KeyboardButton locationButton = new KeyboardButton("Отправить геопозицию");
        locationButton.setRequestLocation(true);
        row.add(locationButton);
        buttons.add(row);
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);
        keyboard.setKeyboard(buttons);
        return keyboard;
    }

    private ReplyKeyboardMarkup getMovieRandomizerKeyboard() {
        List<KeyboardRow> buttons = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        KeyboardButton button = new KeyboardButton("Рандомный фильм");
        row.add(button);
        buttons.add(row);
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);
        keyboard.setKeyboard(buttons);
        return keyboard;
    }


    private ReplyKeyboard getKeyboard(){
        return statesInfo.get(model.getMenuState()).keyboard;
    }

    @Override
    public String getBotUsername() {
        return BotProperties.getProperty("KIShName");
    }

    @Override
    public String getBotToken() {
        return BotProperties.getProperty("KIShToken");
    }
}

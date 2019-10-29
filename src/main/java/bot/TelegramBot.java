package bot;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.logging.Logger;

public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = Logger.getLogger(TelegramBot.class.getName());
    private Model model = new Model();

    public static void main(String[] args) {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try{
            telegramBotsApi.registerBot(new TelegramBot());
        } catch (TelegramApiException e){
            logger.info(e.getMessage());
        }
    }


    @Override
    public void onUpdateReceived(Update update) {
        logger.info("Чат id - " +update.getMessage().getChatId().toString());
        String chatID = update.getMessage().getChatId().toString();
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            String messageText = message.getText();
            MenuState lastState = model.getMenuState();
            model.updateMenuState(messageText);
            if(lastState != model.getMenuState())
                sendMessage(chatID,model.getStateHelloMessage());
            String answer = model.getStateAnswer(messageText);
            if(!answer.isEmpty()) {
                if (model.getMenuState() == MenuState.PhotoGetter)
                    sendPhoto(message, answer);
                else sendMessage(chatID,answer);
            }
            logger.info(model.getMenuState().toString());
        }
    }

    public void sendMessage(String chatId, String text){
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        }catch (TelegramApiException e){
            logger.info(e.getMessage());
        }
    }

    public void sendPhoto(Message message, String url){
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(url);
        sendPhoto.setChatId(message.getChatId().toString());
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            logger.info(e.getMessage());
        }
    }


    @Override
    public String getBotUsername() {
        return "KavoIShoBot";
    }

    @Override
    public String getBotToken() {
        return "811627871:AAGfzGS9KfDq4Rsdbc_kx4CJ65QAnpQO6FA";
    }
}

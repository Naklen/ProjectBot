package bot.games;
import bot.*;
import tools.Parser;

import java.io.*;
import java.util.*;

public class CitiesGame {
    private Bot bot;
    private Random rnd = new Random();
    private String lastWord;
    private Character lastChar;
    private HashMap<Character, ArrayList<String>> wordsMap = Parser.parse(new File(System.getProperty("user.dir") +
            File.separator + "resources" + File.separator + "RussianCities.txt"));

    public CitiesGame(Bot bot) {
        this.bot = bot;
    }

    public void run() {
        bot.printAnswer("Назовите любой город:");
        lastWord = bot.getCommand();
        lastChar = lastWord.toUpperCase().charAt(0);

        while (!bot.isStop()) {
            if (!wordsMap.containsKey(lastChar) ||
                    !wordsMap.get(lastChar).contains(lastWord)) {
                bot.printAnswer("Не-а. Вы проиграли.\n Сыграем еще?\n 1. Да\n 2. Нет");
                String command = bot.getCommand();
                if (command.equals("1")) {
                    this.run();
                    return;
                }
                if (command.equals("2"))
                    return;

            }

            updateLastChar(lastWord);
            wordsMap.get(lastChar).remove(lastWord);

            if (!wordsMap.get(lastChar).isEmpty()) {
                var index = rnd.nextInt(wordsMap.get(lastChar).size() - 1);
                lastWord = wordsMap.get(lastChar).get(index);
                wordsMap.get(lastChar).remove(index);
                bot.printAnswer(lastWord);
                updateLastChar(lastWord);
            } else {
                bot.printAnswer("Я проиграл");
                return;
            }

            lastWord = bot.getCommand();
        }

    }

    private void updateLastChar(String lastWord){
        lastChar = lastWord.toUpperCase().charAt(lastWord.length() - 1);
        if (lastChar == 'Ь' || lastChar == 'Ы' || lastChar == 'Ъ' || lastChar == 'Й')
            lastChar = lastWord.toUpperCase().charAt(lastWord.length() - 2);
    }
}

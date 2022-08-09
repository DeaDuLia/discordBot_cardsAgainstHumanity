package ru.vsu.cs.course1.mavenproject4;


import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class TheCardsAgainstTheHumanityTheGame {
    static JDA jda;
    private List<Player> players = new ArrayList<>();
    private List<QuestionCard> qCards = new ArrayList<>();
    private List<AnswerCard> aCards = new ArrayList<>();
    private List<AnswerCard> playersAnswersInGame = new ArrayList<>();
    private String name;
    private Message playersAnswersMessage;
    private int nullAnswersCount = 0;
    private boolean isInGame = false;
    private static Map<User, TheCardsAgainstTheHumanityTheGame> ownersWithGames = new HashMap<>();
    static private String preff = "==";
    private int masterNumber = -1;
    private Message lobbyMessage;
    private TextChannel room;
    private String guildId;

    private ListenerAdapter la = new ListenerAdapter() {
        @Override
        public void onMessageReceived(MessageReceivedEvent ev) {
            TextChannel textChannel = ev.getMessage().getTextChannel();
            if (room != null && textChannel != null && textChannel.equals(room)) {
                Message msg = ev.getMessage();
                step1(msg);
                try {
                    step2(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                step3(msg);
                step4(msg);
                step5(msg);
            }
        }
    };


    private TheCardsAgainstTheHumanityTheGame(User user, String gameName) {
        this.name = gameName;
        ownersWithGames.put(user, this);
    }

    static void createGame(Message msg) throws Exception {
        if (msg.getContentRaw().startsWith(preff + "create")) {
            if (ownersWithGames.containsKey(msg.getAuthor())) { // Добавить проверку на похожее название
                throw new Exception("Вы уже являетесь создателем игры");
            } else {
                Scanner sc = new Scanner(msg.getContentRaw());
                sc.next();

                String gameName;
                try {
                    StringBuilder sb = new StringBuilder();
                    while (sc.hasNext()) sb.append(sc.next()).append(" ");
                    gameName = sb.toString();
                } catch (Exception e) {
                    throw new Exception("Введите название команты.");
                }
                if (getGameByName(gameName) != null) {
                    throw new Exception("Игра уже создана, присоединитесь");
                }
                TheCardsAgainstTheHumanityTheGame game = new TheCardsAgainstTheHumanityTheGame(msg.getAuthor(), gameName);
                game.players.add(new Player(msg.getAuthor()));
                EmbedBuilder eb = new EmbedBuilder();
                StringBuilder sb = new StringBuilder();
                sb.append("Игроки(").append(game.players.size()).append("/8) \n");
                for (Player player : game.players) {
                    sb.append(player.user.getName()).append("\n");
                }
                eb.setTitle(game.name)
                        .setDescription(sb.toString()).setFooter("Введите " + preff + "join " + gameName + ", чтобы присоединиться к игре", null);

                try {
                    game.lobbyMessage = msg.getTextChannel().sendMessage(eb.build()).complete();
                } catch (Exception e) {
                    ownersWithGames.remove(msg.getAuthor());
                    throw new Exception(e.getMessage());
                }
            }
        }
    }

    static void joinGame(Message msg) throws Exception {
        String text = msg.getContentRaw();
        if (text.startsWith(preff + "join")) {
            if (isPlayer(msg.getAuthor()) || isOwner(msg.getAuthor())) {
                throw new Exception("Вы уже являетесь участником игры, выйдите из неё!");
            } else {
                Scanner sc = new Scanner(text);
                sc.next();
                String gameName;
                try {
                    StringBuilder sb = new StringBuilder();
                    while (sc.hasNext()) sb.append(sc.next()).append(" ");
                    gameName = sb.toString();
                } catch (Exception e) {
                    throw new Exception("Введите название...");
                }
                TheCardsAgainstTheHumanityTheGame game = addPlayerToTheGame(msg, gameName);

                if (game != null && game.isInGame && game.room != null) {
                    game.room.sendMessage("**" + msg.getAuthor().getName() + " уже в игре!**").queue();
                }
            }
        }
    }

    static void leaveGame(Message msg) throws Exception {
        if (msg.getContentRaw().equals(preff + "leave")) {
            User user = msg.getAuthor();

            TheCardsAgainstTheHumanityTheGame game = getGameOfPlayer(user);
            if (game.isInGame && game.room != null) {
                game.room.sendMessage("**" + user.getName() + " покинул игру!**").queue();
                game.room.getPermissionOverride(jda.getGuildById(game.guildId).getMember(user)).delete().queue();
            }
            if (isOwner(user)) {
                if (game.isInGame) {
                    game.room.delete().queue();
                }
                ownersWithGames.remove(user);
            }
            if (game.lobbyMessage != null) {
                TheCardsAgainstTheHumanityTheGame.delPlayer(game, user);
                EmbedBuilder eb = new EmbedBuilder();
                StringBuilder sb = new StringBuilder();
                sb.append("Игроки(").append(game.players.size()).append("/8) \n");
                for (Player player : game.players) {
                    sb.append(player.user.getName()).append("\n");
                }
                eb.setTitle(game.name)
                        .setDescription(sb.toString()).setFooter("Введите " + preff + "join"+ game.name + ", чтобы присоединится к игре", null);
                try {
                    game.lobbyMessage.editMessage(eb.build()).queue();
                } catch (Exception ignored) {

                }
            }

        }
    }

    static void startGame(Message msg, Map<Integer, QuestionCard> questionCardMap, Map<Integer, AnswerCard> answerCardMap) {
        String text = msg.getContentRaw();
        if (text.equals(preff + "start") && ownersWithGames.containsKey(msg.getAuthor())) {
            TheCardsAgainstTheHumanityTheGame game = ownersWithGames.get(msg.getAuthor());
            if (!game.lobbyMessage.getGuild().equals(msg.getGuild())) {
                return;
            }
            if (!game.isInGame) {
                if (game.players.size() >= 1) { // изменить на 3
                    String tChId = msg.getGuild().getController().createTextChannel(game.name).complete().getId();
                    game.room = jda.getTextChannelById(tChId);
                    game.guildId = game.room.getGuild().getId();
                    game.room.createPermissionOverride(game.room.getGuild().getPublicRole()).setDeny(Permission.MESSAGE_READ,
                            Permission.MESSAGE_WRITE).queue();
                    game.createQuestionsList(questionCardMap);
                    game.createAnswersList(answerCardMap);
                    game.isInGame = true;
                    game.startSteps();
                    game.room.sendMessage("Начинаем игру!").queue();
                }
            }
        }
    }

    private void createQuestionsList(Map<Integer, QuestionCard> questionCardMap) {
        qCards.addAll(questionCardMap.values());
    }

    private void createAnswersList(Map<Integer, AnswerCard> answerCardMap) {
        aCards.addAll(answerCardMap.values());
    }

    private void giveToPlayerRandomCards(Player player, int count) {
        List<AnswerCard> playerCards = player.cards;
        for (int i = 0; i < count; i++) {
            if (aCards.size() < 1) return;
            int rnd = rnd(0, aCards.size() - 1);
            playerCards.add(new AnswerCard(aCards.get(rnd).getText()));
            player.cards.get(i).master = player;
            aCards.remove(rnd);
        }
    }

    private void setPermsToRoom(Player player, TextChannel tCh) {
        tCh.createPermissionOverride(tCh.getGuild().getMember(player.user)).setAllow(Permission.MESSAGE_READ,
                Permission.MESSAGE_WRITE,
                Permission.MESSAGE_HISTORY).queue();
    }

    /**
     * Показать игрокам их карты в лс
     */
    private void startSteps() {
        jda.addEventListener(la);
    }

    /**
     * Шаг 1 - восполнить карточки, выбрать следующего ведущего
     *
     * @param msg - сообщение от бота в комнате игры
     */
    private void step1(Message msg) {
        if (!msg.getTextChannel().equals(room) || !msg.getAuthor().getId().equals("660506084666245120") || !msg.getContentRaw().equals("Начинаем игру!")) {
            return;
        }
        for (Player player : players) {
            giveToPlayerRandomCards(player, 9 - player.cards.size());
            if (player.botMessageId == null || player.botMessageId.equals("")) {
                EmbedBuilder eb = new EmbedBuilder();
                for (int i = 0, size = player.cards.size(); i < size; i++) {
                    eb.addField("Карта#" + (i + 1), player.cards.get(i).getText(), true);
                }
                player.eb = eb;
                player.botMessageId = player.user.openPrivateChannel().complete().sendMessage(eb.build()).complete().getId();
                Message msg2 = player.user.openPrivateChannel().complete().getMessageById(player.botMessageId).complete();
                msg2.addReaction("1️⃣").queue();
                msg2.addReaction("2️⃣").queue();
                msg2.addReaction("3️⃣").queue();
                msg2.addReaction("4️⃣").queue();
                msg2.addReaction("5️⃣").queue();
                msg2.addReaction("6️⃣").queue();
                msg2.addReaction("7️⃣").queue();
                msg2.addReaction("8️⃣").queue();
                msg2.addReaction("9️⃣").queue();
            } else {
                for (int i = 0; i < player.eb.getFields().size(); i++) {
                    if (player.eb.getFields().get(i).getValue().equals("xxx")) {
                        int rnd = rnd(0, aCards.size() - 1);
                        try {
                            player.cards.add(i, new AnswerCard(aCards.get(rnd).getText()));
                            aCards.remove(rnd);
                            player.eb.getFields().remove(i);
                            player.eb.getFields().add(i, new MessageEmbed.Field("Карта#" + (i + 1), player.cards.get(i).getText(), true));
                        } catch (Exception ignored) {

                        }
                    }
                }
                player.user.openPrivateChannel().complete().editMessageById(player.botMessageId, player.eb.build()).queue();
            }
            if (this.room.getPermissionOverride(msg.getGuild().getMember(player.user)) == null) {
                setPermsToRoom(player, room);
            }
        }
        masterNumber++;
        if (masterNumber > players.size() - 1) {
            masterNumber = 0;
        }
        room.sendMessage("Ведущий - " + players.get(masterNumber).user.getName()).queue();
        room.sendMessage("Внимание, вопрос!").queue();

    }

    /**
     * Шаг 2 - Вывести в общий чат (Комнату игры) сообщение с вопросом
     *
     * @param msg - сообщение от бота в комнате игры
     */
    private void step2(Message msg) throws Exception {
        if (!msg.getTextChannel().equals(room) || !msg.getAuthor().getId().equals("660506084666245120") || !msg.getContentRaw().equals("Внимание, вопрос!")) {
            return;
        }
        if (qCards.size() < 1) {
            throw new Exception("Карты закончились!");
        }
        int rnd = rnd(0, qCards.size() - 1);
        QuestionCard card = qCards.get(rnd);
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Вопрос:");
        eb.setDescription(card.getText());
        room.sendMessage(eb.build()).queue();
        int[] time = {30};
        Message msg2 = room.sendMessage("Времени на ответ:" + time[0]).complete();

        qCards.remove(rnd);
    }

    /**
     * Шаг 3 - Получать карточки от игроков в лс (По нажатии реакций)
     *
     * @param msg - сообщение от бота в комнате игры
     */
    private void step3(Message msg) {
        if (!msg.getTextChannel().equals(room) || !msg.getAuthor().getId().equals("660506084666245120") || !msg.getContentRaw().startsWith("Времени на ответ:")) {
            return;
        }
        final boolean[] flag = {false};
        final int[] inds = {0};
        final EmbedBuilder eb = new EmbedBuilder();
        playersAnswersInGame.clear();
        eb.setTitle("Ваши ответы");
            /*int i = 5;
            if (i != 25) { // Изменить на проверку индекса ведущего!!!!
                Player player = this.players.get(i);
                Message msg2 = player.user.openPrivateChannel().complete().getMessageById(player.botMessageId).complete();*/
        ListenerAdapter la = new ListenerAdapter() {
            @Override
            public void onPrivateMessageReactionAdd(PrivateMessageReactionAddEvent event) {
                if (event.getReaction().getUsers().complete().get(0).isBot()) return;
                for (AnswerCard ans : playersAnswersInGame) {
                    if (ans.master.user.equals(event.getUser())) return;
                }
                Player player = null;
                for (Player player1 : players) {
                    if (player1.user.equals(event.getUser())) {
                        player = player1;
                    }
                }
                if (player == null || player.user.equals(players.get(masterNumber).user)) return;
                String answer = event.getReactionEmote().getName();
                int answerNumber = 0;
                if (answer.equals("1️⃣")) {
                } else if (answer.equals("2️⃣")) {
                    answerNumber = 1;
                } else if (answer.equals("3️⃣")) {
                    answerNumber = 2;
                } else if (answer.equals("4️⃣")) {
                    answerNumber = 3;
                } else if (answer.equals("5️⃣")) {
                    answerNumber = 4;
                } else if (answer.equals("6️⃣")) {
                    answerNumber = 5;
                } else if (answer.equals("7️⃣")) {
                    answerNumber = 6;
                } else if (answer.equals("8️⃣")) {
                    answerNumber = 7;
                } else if (answer.equals("9️⃣")) {
                    answerNumber = 8;
                }
                AnswerCard card = new AnswerCard(player.cards.get(answerNumber).getText());// Забрать карточку
                card.master = player;
                eb.addField("Ответ#" + (inds[0] + 1), card.getText(), true);
                if (playersAnswersMessage == null) {
                    playersAnswersMessage = room.sendMessage(eb.build()).complete();
                } else {
                    playersAnswersMessage.editMessage(eb.build()).queue();
                }
                playersAnswersInGame.add(card);
                player.cards.remove(answerNumber);
                player.eb.getFields().remove(answerNumber);
                player.eb.getFields().add(answerNumber, new MessageEmbed.Field("Карта#" + (answerNumber + 1), "xxx", true));
                player.user.openPrivateChannel().complete().editMessageById(player.botMessageId, player.eb.build()).queue();
                inds[0]++;
                if (inds[0] >= players.size() - 1 && !flag[0]) {
                    flag[0] = true;
                    jda.removeEventListener(this);
                    room.sendMessage("Выбор ответов завершён!").queue();
                    eb.getFields().clear();

                }
            }
        };
        int[] time = {30};
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> a = service.scheduleWithFixedDelay( // Если выйти здесь, будет выдавать ошибки
                () -> {
                    time[0] -= 5;
                    if (msg == null || this.room == null) return;
                    if (!flag[0]) {
                        msg.editMessage("Времени на ответ: " + time[0]).queue();
                    }
                },
                5,
                5,
                TimeUnit.SECONDS);
        new java.util.Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        a.cancel(true);
                        if (!flag[0]) {
                            flag[0] = true;
                            jda.removeEventListener(la);
                            if (msg != null && room != null) {
                                room.sendMessage("Выбор ответов завершён!").queue();
                                eb.getFields().clear();
                            }
                        }
                    }
                },
                31 * 1000);

        jda.addEventListener(la);
    }

    //Ожидать ответов от игрока
    //Перейти к шагу 4


    /**
     * Шаг 4 - ведущий выбирает лучший вариант
     *
     * @param msg - сообщение от бота в комнате игры
     */
    private void step4(Message msg) {
        if (!msg.getTextChannel().equals(room) || !msg.getAuthor().getId().equals("660506084666245120") || !msg.getContentRaw().equals("Выбор ответов завершён!")) {
            return;
        }
        int sz = playersAnswersInGame.size();
        final boolean[] flag = {false};
        if (sz == 0) {
            room.sendMessage("Подводим итоги...").queue();
            nullAnswersCount++;
            flag[0] = true;
            if (nullAnswersCount >= 3) {
                deleteGame();
            }
        }
        nullAnswersCount = 0;
        playersAnswersMessage = msg;
        if (sz > 0) playersAnswersMessage.addReaction("1️⃣").queue();
        if (sz > 1) playersAnswersMessage.addReaction("2️⃣").queue();
        if (sz > 2) playersAnswersMessage.addReaction("3️⃣").queue();
        if (sz > 3) playersAnswersMessage.addReaction("4️⃣").queue();
        if (sz > 4) playersAnswersMessage.addReaction("5️⃣").queue();
        if (sz > 5) playersAnswersMessage.addReaction("6️⃣").queue();
        if (sz > 6) playersAnswersMessage.addReaction("7️⃣").queue();
        if (sz > 7) playersAnswersMessage.addReaction("8️⃣").queue();

        ListenerAdapter la = new ListenerAdapter() {
            @Override
            public void onMessageReactionAdd(MessageReactionAddEvent event) {
                if (!event.getUser().equals(players.get(masterNumber).user) || flag[0]) {
                    return;
                }
                flag[0] = true;
                String answer = event.getReactionEmote().getName();
                int answerNumber = -1;
                switch (answer) {
                    case "1️⃣":
                        answerNumber = 0;
                        break;
                    case "2️⃣":
                        answerNumber = 1;
                        break;
                    case "3️⃣":
                        answerNumber = 2;
                        break;
                    case "4️⃣":
                        answerNumber = 3;
                        break;
                    case "5️⃣":
                        answerNumber = 4;
                        break;
                    case "6️⃣":
                        answerNumber = 5;
                        break;
                    case "7️⃣":
                        answerNumber = 6;
                        break;
                    case "8️⃣":
                        answerNumber = 7;
                        break;
                    case "9️⃣":
                        answerNumber = 8;
                        break;
                }
                playersAnswersInGame.get(answerNumber).master.points += 1;
                room.sendMessage(players.get(masterNumber).user.getName() + " выбирает ответ игрока " + playersAnswersInGame.get(answerNumber).master.user.getName() + " -- **" + playersAnswersInGame.get(answerNumber).getText() + "**").queue();
                room.sendMessage("Подводим итоги...").queue();
                playersAnswersInGame.clear();
                playersAnswersMessage = null;
                jda.removeEventListener(this);

            }
        };

        new java.util.Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        if (!flag[0]) {
                            flag[0] = true;
                            room.sendMessage("Выберем случайно...").queue();
                            if (sz == 0) {
                                nullAnswersCount++;
                                room.sendMessage("Подводим итоги...").queue();
                                playersAnswersInGame.clear();
                                playersAnswersMessage = null;
                                jda.removeEventListener(la);
                                if (nullAnswersCount >= 3) {
                                    deleteGame();
                                }
                            }

                            int rnd = rnd(0, playersAnswersInGame.size() - 1);
                            playersAnswersInGame.get(rnd).master.points += 1;
                            room.sendMessage("Игра выбирает ответ игрока " + playersAnswersInGame.get(rnd).master.user.getName()).queue();
                            room.sendMessage("Подводим итоги...").queue();
                            playersAnswersInGame.clear();
                            playersAnswersMessage = null;
                            jda.removeEventListener(la);
                        }
                    }
                },
                30 * 1000);
        jda.addEventListener(la);
        //Ожидать ответов от ведущего
        //Перейти к шагу 5
    }

    /**
     * Шаг 5 - подведение итогов 4-х этапов
     *
     * @param msg - сообщение от бота в комнате игры
     */
    private void step5(Message msg) {
        if (!msg.getTextChannel().equals(room) || !msg.getAuthor().getId().equals("660506084666245120") || !msg.getContentRaw().equals("Подводим итоги...")) {
            return;
        }
        EmbedBuilder eb = new EmbedBuilder();
        StringBuilder sb = new StringBuilder();
        boolean flag = false;
        Player winner = null;
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            sb.append(i + 1).append(") ").append(player.user.getName()).append(" - **").append(player.points).append("**\n");
            if (player.points >= 10) { // Установить 10!!!!
                flag = true;
                winner = player;
            }
        }
        eb.setDescription(sb.toString());
        eb.setTitle("Итоги раунда: ");
        room.sendMessage(eb.build()).queue();
        if (flag) {
            room.sendMessage("Поздравим победиля - " + winner.user.getName()).queue();
            new java.util.Timer().schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            jda.removeEventListener(la);
                            deleteGame();
                        }
                    },
                    10 * 1000);
            /*
             * Поздравить победителя +
             * Удалить канал +
             * Удалить игру +
             */
            // Завершить игру
        } else {
            playersAnswersMessage = null;
            playersAnswersInGame.clear();

            room.sendMessage("Начинаем игру!").queue();
        }
        //Подведение итогов
    }
    //=====

    private static boolean isPlayer(User user) {
        for (TheCardsAgainstTheHumanityTheGame game : ownersWithGames.values()) {
            for (int i = 0; i < game.players.size(); i++) {
                if (game.players.get(i).user.equals(user)) {
                    return true;
                }
            }
        }
        return false;

    }

    private static boolean isOwner(User user) {
        return ownersWithGames.containsKey(user);
    }

    private static TheCardsAgainstTheHumanityTheGame getGameOfPlayer(User user) throws Exception {
        for (TheCardsAgainstTheHumanityTheGame game : ownersWithGames.values()) {
            for (Player player : game.players) {
                if (player.user.equals(user)) return game;
            }
        }
        throw new Exception("Нет игры");

    }

    private static User getOwnerOfGame(String gameName) {
        for (User user : ownersWithGames.keySet()) {
            if (ownersWithGames.get(user).name.equals(gameName)) {
                return user;
            }
        }
        return null;
    }

    private static TheCardsAgainstTheHumanityTheGame addPlayerToTheGame(Message msg, String gameName) throws Exception {
        for (TheCardsAgainstTheHumanityTheGame game : ownersWithGames.values()) {
            if (game.name.equals(gameName)) {
                /*if (!game.lobbyMessage.getGuild().equals(msg.getGuild())) {
                    return;
                }*/
                if (game.players.size() >= 8) {
                    throw new Exception("Комната заполнена");
                }
                game.players.add(new Player(msg.getAuthor()));
                EmbedBuilder eb = new EmbedBuilder();
                StringBuilder sb = new StringBuilder();
                sb.append("Игроки(").append(game.players.size()).append("/8) \n");
                for (Player player : game.players) {
                    sb.append(player.user.getName()).append("\n");
                }
                eb.setTitle(game.name)
                        .setDescription(sb.toString()).setFooter("Введите " + preff + "join" +game.name + ", чтобы присоединится к игре", null);
                try {
                    game.lobbyMessage.editMessage(eb.build()).queue();
                } catch (Exception ignored) {

                }
                return game;
            }
        }
        return null;
    }

    private static TheCardsAgainstTheHumanityTheGame getGameByName(String name) {
        for (TheCardsAgainstTheHumanityTheGame game : ownersWithGames.values()) {
            if (game.name.equals(name)) {
                return game;
            }
        }
        return null;
    }

    private static int rnd(int min, int max) {
        max -= min;
        return (int) (Math.random() * ++max) + min;
    }

    private void deleteGame() {
        room.delete().queue();
        ownersWithGames.remove(getOwnerOfGame(name));
        players = null;
        qCards = null;
        aCards = null;
        playersAnswersMessage = null;
        playersAnswersInGame = null;
        name = null;
        room = null;
        guildId = null;
        la = null;
    }
//ssss
/*static void help (Message msg) {
    if (msg.getContentRaw().equals(preff + "help")) {
        msg.getTextChannel().sendMessage("help?").queue();
    }
}*/
    static void help (Message msg) {
        if (msg.getContentRaw().equals(preff+"help")) {
            TextChannel tCh = msg.getTextChannel();
            EmbedBuilder eb = new EmbedBuilder();
            StringBuilder sb = new StringBuilder();
            sb.append("`").append(preff).append("create [name]` - Создать лобби игры\n\n")
                    .append("`").append(preff).append("join [name]` - Присоединиться к лобби игры\n\n")
                    .append("`").append(preff).append("start` - Начать игру\n\n")
                    .append("`").append(preff).append("help` - Показать команды бота\n\n")
                    .append("`").append(preff).append("rules` - Показать правила игры\n\n")
                    .append("=============\n\n")
                    .append("Создать игру/присоединиться к игре -> дождаться, пока наберётся достаточно игроков (3++) --> Начать игру.\n\n")
                    .append("Карточки ответов бот присылает в лс ОДНИМ сообщением и после, с каждым раундом, просто редактирует его.\n\n")
                    .append("Сразу, как только владелец лобби начнёт игру, бот создаст комнату для игры(Там и будет происходить вся игра)\n\n")
                    .append("=============\n\n")
                    .append("Есть вопросы, пожелания или желаете поболтать? Заходи на [наш сервер](https://discord.gg/EPRZAHX)! \n\n" );
                    /*.append(" ( **" + jda.getGuilds().size() + "** серверов)\n\n");*/

            eb.setTitle("Команды бота: ").setDescription(sb.toString()).setColor(Color.GREEN);
            tCh.sendMessage(eb.build()).queue();
        }
    }

    static void rules (Message msg) {
        if (msg.getContentRaw().equals(preff+"rules")) {
            TextChannel tCh = msg.getTextChannel();
            EmbedBuilder eb = new EmbedBuilder();
            StringBuilder sb = new StringBuilder();
            sb.append("1. Каждый раунд разыгрывается вопрос.\n\n")
                    .append("2. Из своих карт, вам нужно выбрать самый смешной ответ.\n\n")
                    .append("3. Ведущий выбирает из предложенных вариантов самый смешной на его взгляд. Этот игрок получает 1 очко.\n\n")
                    .append("4. Далее ведущим становится следующий игрок.\n\n")
                    .append("5. Игрок, набравший 10 очков, побеждает.\n");
            eb.setTitle("Как играть?").setDescription(sb.toString()).setColor(Color.GREEN);
            tCh.sendMessage(eb.build()).queue();
        }
    }

    private static void delPlayer (TheCardsAgainstTheHumanityTheGame game, User user) {
        for (Player player : game.players) {
            if (player.user.equals(user)) {
                game.players.remove(player);
                return;
            }
        }
    }

}

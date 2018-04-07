package com.polis.news.app.controller;

import com.polis.news.app.model.User;
import com.polis.news.app.pojo.Articles;
import com.polis.news.app.repository.UserRepository;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Controller
@RequestMapping(path = "/")
public class AppController {

    @Autowired
    private UserRepository userRepository;

    private static final String sourceAPI = "https://newsapi.org/v2/top-headlines?sources=";
    private static final String apiKey = "3450b17dc0914a42ab0882c38e083c6b";

    @GetMapping("/")
    public String login(){

        return "login";
    }

      @GetMapping("/logout")
      public String logout(HttpServletRequest request){
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if(user != null)
            session.setAttribute("user",null);
        session.invalidate();
        return "redirect:/";

    }


    @GetMapping("/welcome")
    public String welcome( HttpServletRequest request){

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if(user == null)
            return "redirect:/";

        return "welcome";
    }

    @PostMapping("/welcome")
    public String welcome(@RequestParam(value = "email") String email,
                          @RequestParam(value = "password") String password, HttpServletRequest request, Model model){
        if(!email.isEmpty() && !password.isEmpty()){

            HttpSession session = request.getSession();

            User user = findUserByEmail(email);
            if(user != null){

                session.setAttribute("user",user);
                model.addAttribute("email",user.getEmail());

            }else{

                user = new User();
                user.setEmail(email);
                user.setPassword(password);
                User savedUser = userRepository.save(user);
                session.setAttribute("user",savedUser);
                model.addAttribute("email",savedUser.getEmail());

            }

            return "welcome";

        }
        return "redirect:/";
    }

    @GetMapping("/top-headlines")
    public String topHeadlines(@RequestParam(value = "source") String source, Model model, HttpServletRequest request){

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if(user == null)
            return "redirect:/";

        model.addAttribute("email",user.getEmail());

        String apiURL = sourceAPI+ source + "&apiKey="+ apiKey;

        String data = "";

        try {
            URL url = new URL(apiURL);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            int responseCode = connection.getResponseCode();

            if(responseCode != 200)
                throw new RuntimeException("HttpResponseCode = " +responseCode);
            else{
                Scanner sc = new Scanner(url.openStream());
                while(sc.hasNext()){
                    data += sc.nextLine();
                }
                System.out.println("JSON data");
                System.out.println(data);
                sc.close();
            }

            List<Articles> articlesList = new ArrayList<>();

            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(data);

            JSONArray articlesArr = (JSONArray) jsonObject.get("articles");

            for(int i=0; i<articlesArr.size(); i++){

                JSONObject articleObj = (JSONObject) articlesArr.get(i);

                Articles articles = new Articles();

                JSONObject sourceObj = (JSONObject) articleObj.get("source");
                articles.setSource((String) sourceObj.get("name"));

                articles.setAuthor((String) articleObj.get("author"));
                articles.setTitle((String) articleObj.get("title"));
                articles.setDescription((String) articleObj.get("description"));
                articles.setUrl((String) articleObj.get("url"));
                articles.setUrlToImage((String) articleObj.get("urlToImage"));
                articlesList.add(articles);
            }

            model.addAttribute("articles",articlesList);
            model.addAttribute("source",articlesList.get(0).getSource());




        } catch (Exception e) {
            e.printStackTrace();
        }

        return "top-headlines";
    }


    public User findUserByEmail(String email){

        Iterable<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (user.getEmail().equals(email)) {
                return user;
            }
        }
        return null;

    }




}

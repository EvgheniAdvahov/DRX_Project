package com.drx.Project.controllers;

import com.drx.Project.model.Post;
import com.drx.Project.model.PostDto;
import com.drx.Project.model.User;
import com.drx.Project.service.PostService;
import com.drx.Project.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@AllArgsConstructor
@Controller
@RequestMapping("/posts")
public class PostController {

    private final UserService userService;
    private final PostService postService;

    @GetMapping
    public String showPostsPage(Model model, Principal principal) {
        model.addAttribute("username", getUserFullName(principal));
        List<Post> posts = postService.getAllPosts();
        model.addAttribute("posts", posts);

        return "posts/postList";
    }

    @GetMapping("/createPost")
    public String newPostPage(Model model, Principal principal) {
        model.addAttribute("postDto", new PostDto());
        //Add user full name to html page
        model.addAttribute("username", getUserFullName(principal));
        return "posts/createPost";
    }

    @PostMapping("/createPost")
    public String createNewPost(@Valid @ModelAttribute PostDto postDto,
                                BindingResult result,
                                Model model,
                                Principal principal) {
        //Add user full name to html page
        model.addAttribute("username", getUserFullName(principal));

        if (result.hasErrors()) {
            return "posts/createPost";
        }

        Post post = new Post();
        post.setTitle(postDto.getTitle());
        post.setContent(postDto.getContent());
        post.setCreatedAt(dateTime());
        post.setCreatedBy(getUserFullName(principal));

        postService.savePostToDb(post);

        return "redirect:/posts";
    }

    @GetMapping("/editPost")
    public String editPostPage(Model model, @RequestParam int id, Principal principal) {
        model.addAttribute("username", getUserFullName(principal));
        Post post = postService.getById(id).get();

        PostDto postDto = new PostDto();
        postDto.setTitle(post.getTitle());
        postDto.setContent(post.getContent());

        model.addAttribute("postDto", postDto);

        return "posts/editPost";
    }

    @PostMapping("editPost")
    public String editPost(@RequestParam int id,
                           @Valid @ModelAttribute PostDto postDto,
                           BindingResult result,
                           Model model,
                           Principal principal) {
        model.addAttribute("username", getUserFullName(principal));
        if (result.hasErrors()) {
            return "posts/editPost";
        }

        Post post = postService.getById(postDto.getId()).get();
        post.setTitle(postDto.getTitle());
        post.setContent(postDto.getContent());
        post.setModifiedAt(dateTime());
        post.setModifiedBy(getUserFullName(principal));


        postService.savePostToDb(post);

        return "redirect:/posts";

    }

    @GetMapping("deletePost")
    public String deletePost(@RequestParam int id) {
        postService.deleteById(id);
        return "redirect:/posts";
    }


    //Date and time
    private String dateTime() {
        LocalDateTime createdAt = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH∶mm∶ss");
        return createdAt.format(formatter);
    }

    private String getUserFullName(Principal principal) {
        User user = userService.getUserByUsername(principal.getName()).get();
        return user.getFullName();
    }
}

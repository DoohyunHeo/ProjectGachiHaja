package com.example.projectgachihaja.domain.Together.controller;

import com.example.projectgachihaja.domain.Post.Post;
import com.example.projectgachihaja.domain.Post.PostForm;
import com.example.projectgachihaja.domain.Post.PostRepository;
import com.example.projectgachihaja.domain.Post.PostService;
import com.example.projectgachihaja.domain.Together.Together;
import com.example.projectgachihaja.domain.Together.TogetherRepository;
import com.example.projectgachihaja.domain.Together.TogetherService;
import com.example.projectgachihaja.domain.account.Account;
import com.example.projectgachihaja.domain.account.CurrentAccount;
import com.example.projectgachihaja.domain.comment.Comment;
import com.example.projectgachihaja.domain.comment.CommentForm;
import com.example.projectgachihaja.domain.comment.CommentRepository;
import com.example.projectgachihaja.domain.comment.CommentService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class TogetherBoardController {
    private final ModelMapper modelMapper;
    private final TogetherRepository togetherRepository;
    private final TogetherService togetherService;
    private final PostService postService;
    private final PostRepository postRepository;
    private final CommentService commentService;
    private final CommentRepository commentRepository;

    @GetMapping("/together/{path}/board")
    public String togetherBoardView(@CurrentAccount Account account, @PathVariable String path,
                                    @PageableDefault(size = 9,sort="reportingDate", direction = Sort.Direction.DESC) Pageable pageable, Model model){
        Together together = togetherRepository.findWithPostsByPath(path);
        List<Post> postList = List.copyOf(together.getPosts("user"));
        int start = (int)pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), postList.size());
        Page<Post> postPage = new PageImpl<>(postList.subList(start, end), pageable, postList.size());

        model.addAttribute(together);
        model.addAttribute(account);
        model.addAttribute("noticePost", together.getPosts("notice"));
        model.addAttribute("postPage",postPage);
        model.addAttribute("sortProperty", pageable.getSort().toString().contains("reportingDate") ? "reportingDate" : "memberCount");

        return "together/board/board";
    }

    @GetMapping("/together/{path}/board/create")
    public String togetherPostCreate(@CurrentAccount Account account, @PathVariable String path, Model model){
        Together together = togetherRepository.findWithPostsByPath(path);
        if(!togetherService.permissionCheck(together,account)){
            return "redirect:/together/" +together.pathEncoder() + "/error";
        }
        PostForm postForm = new PostForm();
        model.addAttribute(together);
        model.addAttribute(account);
        model.addAttribute(postForm);

        return "together/board/create";
    }

    @PostMapping("/together/{path}/board/create")
    public String togetherPostCreateComplete(@CurrentAccount Account account, @PathVariable String path, PostForm postForm){
        Together together = togetherRepository.findByPath(path);
        Post newPost = postService.createNewPost(postForm,together, account);
        togetherService.newPostRegister(together,newPost);

        return "redirect:/together/" +together.pathEncoder() + "/board/" +newPost.getId();
    }

    @GetMapping("/together/{path}/board/{postid}")
    public String postView(@CurrentAccount Account account, @PathVariable String path,@PathVariable Long postid, Model model){
        Together together = togetherRepository.findWithPostsByPath(path);
        if(!togetherService.permissionCheck(together,account)){
            return "redirect:/together/" +together.pathEncoder() + "/error";
        }
        Post post = postRepository.findWithCommentsById(postid);
        CommentForm commentForm = new CommentForm();

        postService.updateViewer(post,account);
        model.addAttribute(together);
        model.addAttribute(post);
        model.addAttribute(commentForm);
        model.addAttribute(account);

        return "together/board/view";
    }

    @GetMapping("/together/{path}/board/{postid}/edit")
    public String togetherPostEdit(@CurrentAccount Account account, @PathVariable String path,@PathVariable Long postid ,Model model){
        Together together = togetherRepository.findWithPostsByPath(path);
        Post post = postRepository.findWithCommentsById(postid);
        if(!post.getWriter().getNickname().equals(account.getNickname())){
            return "redirect:/together/" +together.pathEncoder() + "/board";
        }
        PostForm postForm = modelMapper.map(post,PostForm.class);
        model.addAttribute(post);
        model.addAttribute(together);
        model.addAttribute(postForm);
        model.addAttribute(account);

        return "together/board/edit";
    }

    @PostMapping("/together/{path}/board/{postid}/edit")
    public String togetherPostEditComplete(@CurrentAccount Account account, @PathVariable String path,@PathVariable Long postid, PostForm postForm){
        Together together = togetherRepository.findWithPostsByPath(path);
        Post post = postRepository.findWithCommentsById(postid);
        postService.postEdit(post, postForm);

        return "redirect:/together/"+together.pathEncoder() +"/board/" + post.getId();
    }

    @GetMapping("/together/{path}/board/search")
    public String postSearch(@CurrentAccount Account account, @PathVariable String path,
                             @PageableDefault(size = 9,sort="reportingDate", direction = Sort.Direction.DESC) Pageable pageable,String keyword,String type, Model model){
        Together together = togetherRepository.findWithPostsByPath(path);
        if(type.equals("title")) {
            Page<Post> postPage = postRepository.findWithPostTitleByKeyword(keyword, pageable);
            model.addAttribute("postPage",postPage);
        }
        else {
            Page<Post> postPage = postRepository.findWithWriterByKeyword(keyword, pageable);
            model.addAttribute("postPage",postPage);
        }
        model.addAttribute(together);
        model.addAttribute("keyword",keyword);

        return "together/board/board";
    }



    @PostMapping("/together/{path}/board/{postid}/remove")
    public String postRemove(@CurrentAccount Account account, @PathVariable String path, @PathVariable Long postid){
        Together together = togetherRepository.findWithPostsByPath(path);
        Post post = postRepository.findWithCommentsById(postid);
        Set<Comment> comments = post.getComments();
        togetherService.postDelete(together, post);
        postService.postDelete(post,account);
        commentService.allCommentsRemove(comments);

        return "redirect:/together/" +together.pathEncoder() + "/board/";
    }

    @PostMapping("/together/{path}/board/{postid}/comment")
    public String commentWrite(@CurrentAccount Account account, @PathVariable String path, @PathVariable Long postid, CommentForm commentForm){
        Together together = togetherRepository.findWithPostsByPath(path);
        Post post = postRepository.findWithCommentsById(postid);
        Comment newComment = commentService.newCommentWrite(account, commentForm);
        postService.newCommentRegister(together,post,newComment);

        return "redirect:/together/" +together.pathEncoder() + "/board/" + post.getId();
    }

    @PostMapping("/together/{path}/board/{postid}/comment/{commentid}/edit")
    public String commentEdit(@CurrentAccount Account account, @PathVariable String path, @PathVariable Long postid, @PathVariable Long commentid, CommentForm commentForm){
        Together together = togetherRepository.findWithPostsByPath(path);
        Post post = postRepository.findWithCommentsById(postid);
        Optional<Comment> comment = commentRepository.findById(commentid);
        commentService.commentEdit(comment.orElseThrow(), commentForm);

        return "redirect:/together/"+together.pathEncoder() +"/board/" + post.getId();
    }

    @PostMapping("/together/{path}/board/{postid}/comment/{commentid}/remove")
    public String commentRemove(@CurrentAccount Account account, @PathVariable String path, @PathVariable Long postid, @PathVariable Long commentid){
        Together together = togetherRepository.findWithPostsByPath(path);
        Post post = postRepository.findWithCommentsById(postid);
        Optional<Comment> comment = commentRepository.findById(commentid);
        postService.commentRemove(post,comment.orElseThrow());
        commentService.commentRemove(account,comment.orElseThrow());

        return "redirect:/together/" +together.pathEncoder() + "/board/" + post.getId();
    }
}

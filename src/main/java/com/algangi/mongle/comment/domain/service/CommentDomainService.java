package com.algangi.mongle.comment.domain.service;

import org.springframework.stereotype.Service;

import com.algangi.mongle.comment.domain.model.Comment;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.domain.model.Post;

@Service
public class CommentDomainService {

    public Comment createParentComment(Post post, Member author, String content, boolean isAnonymous) {
        return Comment.createParentComment(content, post, author, isAnonymous);
    }

    public Comment createChildComment(Comment parent, Member author, String content, boolean isAnonymous) {
        return Comment.createChildComment(content, parent, author, isAnonymous);
    }

    public void deleteComment(Comment comment) {
        comment.softDeleteByUser();
    }

}
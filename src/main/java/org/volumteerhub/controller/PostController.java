package org.volumteerhub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.volumteerhub.common.validation.OnCreate;
import org.volumteerhub.common.validation.OnUpdate;
import org.volumteerhub.dto.PostDto;
import org.volumteerhub.service.PostService;

import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // LIST
    @GetMapping("/events/{eventId}/posts")
    public ResponseEntity<PagedModel<EntityModel<PostDto>>> listByEvent(
            @PathVariable UUID eventId,
            Pageable pageable,
            PagedResourcesAssembler<PostDto> assembler) {

        Page<PostDto> page = postService.listByEvent(eventId, pageable);

        PagedModel<EntityModel<PostDto>> resources = assembler.toModel(page, dto ->
                EntityModel.of(dto,
                        linkTo(methodOn(PostController.class).getPost(dto.getId())).withSelfRel()
                )
        );

        return ResponseEntity.ok(resources);
    }

    // CREATE
    @PostMapping("/events/{eventId}/posts")
    public ResponseEntity<PostDto> createPost(
            @PathVariable UUID eventId,
            @Validated(OnCreate.class) @RequestBody PostDto dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.create(eventId, dto));
    }

    // GET
    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostDto> getPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(postService.get(postId));
    }

    // UPDATE
    @PatchMapping("/posts/{postId}")
    public ResponseEntity<PostDto> updatePost(
            @PathVariable UUID postId,
            @Validated(OnUpdate.class) @RequestBody PostDto dto) {
        return ResponseEntity.ok(postService.update(postId, dto));
    }

    // DELETE
    @DeleteMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable UUID postId) {
        postService.delete(postId);
    }
}

package socialnet.service;

import lombok.RequiredArgsConstructor;
import socialnet.model.entities.Post;
import socialnet.model.entities.Tag;
import socialnet.repository.TagsRepository;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagsService {

    private final TagsRepository tagRepository;

    private Tag createTag(String tagName) {
        Tag tag = new Tag(tagName.replaceAll("[^ёЁа-яА-Яa-zA-Z_0-9-]+", ""));
        if (tag.getTagName().isEmpty()) {
            return null;
        }
        return tagRepository.save(tag);
    }

    private Tag getTagByTagName(String tagName) {
        return tagRepository.findByTagName(tagName).orElseGet(() -> createTag(tagName));
    }

    @Named("getTagsByStrings")
    public List<Tag> stringsToTagsMapper(List<String> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }
        return tags.stream().map(this::getTagByTagName).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Named("getStringsByTags")
    public List<String> tagsToStringsMapper(List<Tag> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }
        return tags.stream().map(Tag::getTagName).collect(Collectors.toList());
    }

    public Tag addPostToTag(Tag tag, Post post) {
        List<Post> posts = new ArrayList<>();
        if (tag.getPosts() != null) {
            if (tag.getPosts().contains(post)) {
                return tag;
            }
            posts.addAll(tag.getPosts());
        }
        posts.add(post);
        tag.setPosts(posts);
        return tag;
    }

    public Tag dropPostFromTag(Tag tag, Post post) {
        List<Post> posts = new ArrayList<>(tag.getPosts());
        posts.remove(post);
        tag.setPosts(posts);
        return tagRepository.save(tag);
    }
}

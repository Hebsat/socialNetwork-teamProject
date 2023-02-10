package socialnet.service.search;

import org.springframework.data.jpa.domain.Specification;
import socialnet.model.entities.Person;
import socialnet.model.entities.Post;
import socialnet.model.entities.Tag;

import javax.persistence.criteria.Join;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PostSpecification {

    public static Specification<Post> isBlockedFalse() {
        return (root, query, cb) -> cb.equal(root.get("isBlocked"), false);
    }

    public static Specification<Post> textLike(String text) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("postText")), "%" + text.toLowerCase() + "%");
    }

    public static Specification<Post> tagsContains(List<String> tags) {
        return (root, query, cb) -> {
            List<String> lowerCaseTags = new ArrayList<>();
            tags.forEach(tag -> lowerCaseTags.add(tag.toLowerCase()));
            Join<Tag, Post> tagsPost = root.join("tags");
            query.groupBy(root.get("id")).having(cb.equal(cb.count(tagsPost.get("id")), tags.size()));
            return cb.lower(tagsPost.get("tagName")).in(lowerCaseTags);
        };
    }

    public static Specification<Post> datesBetween(LocalDateTime dateFrom, LocalDateTime dateTo) {
        return (root, query, cb) -> cb.between(root.get("time"), dateFrom, dateTo);
    }

    public static Specification<Post> postAuthorsIs(List<Person> authors) {
        return (root, query, cb) -> cb.in(root.get("author")).value(authors);
    }

    public static Specification<Post> excludeBlockedPosts(List<Person> personsWhoBLockedMe) {
        return (root, query, cb) -> cb.in(root.get("author")).value(personsWhoBLockedMe).not();
    }
}

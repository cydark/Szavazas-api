package hu.ogyh.voting.repository;

/**
 * 5.1 alapadatai egy sorban: a leadott szavazatok száma és a szavazó képviselők száma. A képviselőnkénti
 * részvételek átlaga ezek hányadosa.
 */
public record ParticipationTotals(long voteCount, long memberCount) {}

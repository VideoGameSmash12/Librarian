package me.videogamesm12.librarian.api.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;

@Getter
@RequiredArgsConstructor
public class SaveFailureEvent extends LibrarianEvent
{
	private final IWrappedHotbarStorage storage;

	private final Throwable error;
}

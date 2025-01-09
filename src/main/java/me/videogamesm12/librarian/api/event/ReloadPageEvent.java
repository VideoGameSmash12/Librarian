package me.videogamesm12.librarian.api.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigInteger;

@Getter
@RequiredArgsConstructor
public class ReloadPageEvent extends LibrarianEvent
{
	private final BigInteger currentPage;
}

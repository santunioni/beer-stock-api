package one.digitalinnovation.beerstock.service;

import one.digitalinnovation.beerstock.builder.BeerDTOBuilder;
import one.digitalinnovation.beerstock.dto.BeerDTO;
import one.digitalinnovation.beerstock.entity.Beer;
import one.digitalinnovation.beerstock.exception.BeerAlreadyRegisteredException;
import one.digitalinnovation.beerstock.exception.BeerNotFoundException;
import one.digitalinnovation.beerstock.exception.BeerStockEmptyException;
import one.digitalinnovation.beerstock.exception.BeerStockExceededException;
import one.digitalinnovation.beerstock.mapper.BeerMapper;
import one.digitalinnovation.beerstock.repository.BeerRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BeerServiceTest {

    private static final long INVALID_BEER_ID = 1L;

    @Mock
    private BeerRepository beerRepository;

    private final BeerMapper beerMapper = BeerMapper.INSTANCE;

    @InjectMocks
    private BeerService beerService;

    private final BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
    private final Beer beer = beerMapper.toModel(beerDTO);

    @Test
    void whenBeerInformedThenItShouldBeCreated() throws BeerAlreadyRegisteredException {
        // when
        when(beerRepository.findByName(beerDTO.getName())).thenReturn(Optional.empty());
        when(beerRepository.save(beer)).thenReturn(beer);

        //then
        BeerDTO createdBeerDTO = beerService.createBeer(beerDTO);

        assertThat(createdBeerDTO.getId()).isEqualTo(beerDTO.getId());
        assertThat(createdBeerDTO.getName()).isEqualTo(beerDTO.getName());
        assertThat(createdBeerDTO.getQuantity()).isEqualTo(beerDTO.getQuantity());
    }

    @Test
    void whenAlreadyRegisteredBeerInformedThenAnExceptionShouldBeThrown() {
        // when
        when(beerRepository.findByName(beerDTO.getName())).thenReturn(Optional.of(beer));

        // then
        assertThatThrownBy(() -> beerService.createBeer(beerDTO))
                .isInstanceOf(BeerAlreadyRegisteredException.class);
    }

    @Test
    void whenValidBeerNameIsGivenThenReturnABeer() throws BeerNotFoundException {
        // when
        when(beerRepository.findByName(beer.getName())).thenReturn(Optional.of(beer));

        // then
        assertThat(beerService.findByName(beerDTO.getName())
        ).isEqualTo(beerDTO);
    }

    @Test
    void whenNotRegisteredBeerNameIsGivenThenThrowAnException() {
        // when
        when(beerRepository.findByName(beerDTO.getName())).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> beerService.findByName(beerDTO.getName()))
                .isInstanceOf(BeerNotFoundException.class);
    }

    @Test
    void whenListBeerIsCalledThenReturnAListOfBeers() {
        //when
        when(beerRepository.findAll()).thenReturn(Collections.singletonList(beer));

        //then
        assertThat(beerService.listAll()).isNotEmpty();
        assertThat(beerService.listAll().get(0)).isEqualTo(beerDTO);
    }

    @Test
    void whenListBeerIsCalledThenReturnAnEmptyList() {
        //when
        when(beerRepository.findAll()).thenReturn(Collections.emptyList());

        //then
        assertThat(beerService.listAll()).isEmpty();
    }

    @Test
    void whenExclusionIsCalledWithValidIdThenABeerShouldBeDeleted() throws BeerNotFoundException {
        // when
        when(beerRepository.findById(beerDTO.getId())).thenReturn(Optional.of(beer));
        doNothing().when(beerRepository).deleteById(beerDTO.getId());

        // then
        beerService.deleteById(beerDTO.getId());

        verify(beerRepository).findById(beerDTO.getId());
        verify(beerRepository).deleteById(beerDTO.getId());
    }

    @Test
    void whenIncrementIsCalledThenIncrementBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        //given
        int quantityToIncrement = 10;
        int expectedQuantityAfterIncrement = beerDTO.getQuantity() + quantityToIncrement;

        //when
        when(beerRepository.findById(beerDTO.getId())).thenReturn(Optional.of(beer));
        when(beerRepository.save(beer)).thenReturn(beer);

        // then
        BeerDTO incrementedBeerDTO = beerService.increment(beerDTO.getId(), quantityToIncrement);
        assertThat(expectedQuantityAfterIncrement).isEqualTo(incrementedBeerDTO.getQuantity());
        assertThat(expectedQuantityAfterIncrement).isLessThan(beerDTO.getMax());
    }

    @Test
    void whenIncrementIsGreaterThanMaxThenThrowException() {
        //when
        when(beerRepository.findById(beerDTO.getId())).thenReturn(Optional.of(beer));

        //then
        assertThatThrownBy(() -> beerService.increment(beerDTO.getId(), beer.getMax() + 1)
        ).isInstanceOf(BeerStockExceededException.class);
    }

    @Test
    void whenIncrementAfterSumIsGreaterThanMaxThenThrowException() {
        //when
        when(beerRepository.findById(beerDTO.getId())).thenReturn(Optional.of(beer));

        //then
        assertThatThrownBy(() -> beerService.increment(
                beerDTO.getId(), beer.getMax() - beer.getQuantity() + 1)
        ).isInstanceOf(BeerStockExceededException.class);
    }

    @Test
    void whenIncrementIsCalledWithInvalidIdThenThrowException() {
        //when
        when(beerRepository.findById(INVALID_BEER_ID)).thenReturn(Optional.empty());

        //then
        assertThatThrownBy(() -> beerService.increment(INVALID_BEER_ID, 10)
        ).isInstanceOf(BeerNotFoundException.class);
    }

    @Test
    void whenDecrementIsCalledThenDecrementBeerStock() throws BeerNotFoundException, BeerStockEmptyException {
        //given
        int quantityToDecrement = beerDTO.getQuantity() / 2;
        int expectedQuantityAfterDecrement = beerDTO.getQuantity() - quantityToDecrement;

        //when
        when(beerRepository.findById(beerDTO.getId())).thenReturn(Optional.of(beer));
        when(beerRepository.save(beer)).thenReturn(beer);

        //then
        BeerDTO decrementedBeerDTO = beerService.decrement(beerDTO.getId(), quantityToDecrement);
        assertThat(expectedQuantityAfterDecrement).isEqualTo(decrementedBeerDTO.getQuantity());
        assertThat(expectedQuantityAfterDecrement).isGreaterThan(0);
    }

    @Test
    void whenDecrementIsCalledToEmptyStockThenEmptyBeerStock() throws BeerNotFoundException, BeerStockEmptyException {
        //when
        when(beerRepository.findById(beerDTO.getId())).thenReturn(Optional.of(beer));
        when(beerRepository.save(beer)).thenReturn(beer);

        //then
        BeerDTO decrementedBeerDTO = beerService.decrement(beerDTO.getId(), beerDTO.getQuantity());
        assertThat(decrementedBeerDTO.getQuantity()).isEqualTo(0);
    }

    @Test
    void whenDecrementIsHigherThanQuantityThenThrowException() {
        // when
        when(beerRepository.findById(beerDTO.getId())).thenReturn(Optional.of(beer));

        //then
        assertThatThrownBy(() -> beerService.decrement(beerDTO.getId(), 2 * beer.getQuantity())
        ).isInstanceOf(BeerStockEmptyException.class);
    }

    @Test
    void whenDecrementIsCalledWithInvalidIdThenThrowException() {
        //when
        when(beerRepository.findById(INVALID_BEER_ID)).thenReturn(Optional.empty());

        //then
        assertThatThrownBy(() -> beerService.decrement(INVALID_BEER_ID, 10)
        ).isInstanceOf(BeerNotFoundException.class);
    }
}

package one.digitalinnovation.beerstock.service;

import one.digitalinnovation.beerstock.builder.BeerDTOBuilder;
import one.digitalinnovation.beerstock.dto.BeerDTO;
import one.digitalinnovation.beerstock.entity.Beer;
import one.digitalinnovation.beerstock.exception.BeerAlreadyRegisteredException;
import one.digitalinnovation.beerstock.exception.BeerNotFoundException;
import one.digitalinnovation.beerstock.exception.BeerStockExceededException;
import one.digitalinnovation.beerstock.mapper.BeerMapper;
import one.digitalinnovation.beerstock.repository.BeerRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
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
        // given
        Beer expectedSavedBeer = beer;

        // when
        when(beerRepository.findByName(beerDTO.getName())).thenReturn(Optional.empty());
        when(beerRepository.save(expectedSavedBeer)).thenReturn(expectedSavedBeer);

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
        // given (DAMP over DRY)
        BeerDTO expectedFoundBeerDTO = beerDTO;
        Beer expectedFoundBeer = beer;

        // when
        when(beerRepository.findByName(expectedFoundBeer.getName())).thenReturn(Optional.of(expectedFoundBeer));

        // then
        BeerDTO foundBeerDTO = beerService.findByName(expectedFoundBeerDTO.getName());

        assertThat(foundBeerDTO).isEqualTo(expectedFoundBeerDTO);
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
        //when
        when(beerRepository.findById(beerDTO.getId())).thenReturn(Optional.of(beer));
        when(beerRepository.save(beer)).thenReturn(beer);

        int quantityToIncrement = 10;
        int expectedQuantityAfterIncrement = beerDTO.getQuantity() + quantityToIncrement;

        // then
        BeerDTO incrementedBeerDTO = beerService.increment(beerDTO.getId(), quantityToIncrement);

        assertThat(expectedQuantityAfterIncrement).isEqualTo(incrementedBeerDTO.getQuantity());
        assertThat(expectedQuantityAfterIncrement).isLessThan(beerDTO.getMax());
    }

    @Test
    void whenIncrementIsGreaterThanMaxThenThrowException() {
        //when
        when(beerRepository.findById(beerDTO.getId())).thenReturn(Optional.of(beer));

        int quantityToIncrement = 80;
        assertThatThrownBy(() -> beerService.increment(beerDTO.getId(), quantityToIncrement))
                .isInstanceOf(BeerStockExceededException.class);
    }

    @Test
    void whenIncrementAfterSumIsGreaterThanMaxThenThrowException() {
        //when
        when(beerRepository.findById(beerDTO.getId())).thenReturn(Optional.of(beer));

        int quantityToIncrement = 45;
        assertThatThrownBy(() -> beerService.increment(beerDTO.getId(), quantityToIncrement))
                .isInstanceOf(BeerStockExceededException.class);
    }

    @Test
    void whenIncrementIsCalledWithInvalidIdThenThrowException() {
        int quantityToIncrement = 10;

        when(beerRepository.findById(INVALID_BEER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> beerService.increment(INVALID_BEER_ID, quantityToIncrement))
                .isInstanceOf(BeerNotFoundException.class);
    }

    @Test
    void whenDecrementIsCalledThenDecrementBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
        when(beerRepository.save(expectedBeer)).thenReturn(expectedBeer);

        int quantityToDecrement = 5;
        int expectedQuantityAfterDecrement = expectedBeerDTO.getQuantity() - quantityToDecrement;
        BeerDTO incrementedBeerDTO = beerService.decrement(expectedBeerDTO.getId(), quantityToDecrement);

        assertThat(expectedQuantityAfterDecrement).isEqualTo(incrementedBeerDTO.getQuantity());
        assertThat(expectedQuantityAfterDecrement).isGreaterThan(0);
    }
//
//    @Test
//    void whenDecrementIsCalledToEmptyStockThenEmptyBeerStock() throws BeerNotFoundException, BeerStockExceededException {
//        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
//        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);
//
//        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
//        when(beerRepository.save(expectedBeer)).thenReturn(expectedBeer);
//
//        int quantityToDecrement = 10;
//        int expectedQuantityAfterDecrement = expectedBeerDTO.getQuantity() - quantityToDecrement;
//        BeerDTO incrementedBeerDTO = beerService.decrement(expectedBeerDTO.getId(), quantityToDecrement);
//
//        assertThat(expectedQuantityAfterDecrement, equalTo(0));
//        assertThat(expectedQuantityAfterDecrement, equalTo(incrementedBeerDTO.getQuantity()));
//    }
//
//    @Test
//    void whenDecrementIsLowerThanZeroThenThrowException() {
//        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
//        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);
//
//        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
//
//        int quantityToDecrement = 80;
//        assertThrows(BeerStockExceededException.class, () -> beerService.decrement(expectedBeerDTO.getId(), quantityToDecrement));
//    }
//
//    @Test
//    void whenDecrementIsCalledWithInvalidIdThenThrowException() {
//        int quantityToDecrement = 10;
//
//        when(beerRepository.findById(INVALID_BEER_ID)).thenReturn(Optional.empty());
//
//        assertThrows(BeerNotFoundException.class, () -> beerService.decrement(INVALID_BEER_ID, quantityToDecrement));
//    }
}

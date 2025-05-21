import {isValidUrl, hasValidVariables, isMagicUrl, validVariables} from "./calendar_url";
import {describe, expect, test} from 'vitest'


describe('check URLs', () => {
    test('empty', () => {
        expect(isValidUrl('')).toBeFalsy()
    })
    test('null', () => {
        expect(isValidUrl('')).toBeFalsy()
    })
    test('just a string', () => {
        expect(isValidUrl('hello world')).toBeFalsy()
    })
    
    test('normal URL', () => {
        expect(isValidUrl('http://site.test/path')).toBeTruthy()
    })
    test('special protocol URL', () => {
        expect(isValidUrl('calendar://test')).toBeTruthy()
    })
})

describe('magic URL detection', () => {
    test('normal URL', () => {
        expect(isMagicUrl('http://site.test')).toBeFalsy()
    })
    test('secure URL', () => {
        expect(isMagicUrl('https://site.test')).toBeFalsy()
    })
    test('calendar URL case sensitive', () => {
        expect(isMagicUrl('CALENDAR://test')).toBeFalsy()
    })
    
    test('calendar URL', () => {
        expect(isMagicUrl('calendar://test')).toBeTruthy()
    })
})

describe('valid variables in URL', () => {
    test('no variables', () => {
        expect(hasValidVariables('http://site.test/')).toBeTruthy()
    })
    test('known variable', () => {
        expect(hasValidVariables('http://sites.test/${course.id}/')).toBeTruthy()
    })
    test('repeated variable', () => {
        expect(hasValidVariables('http://sites.test/${course.id}/${course.id}')).toBeTruthy()
    })
    test('unknown variable', () => {
        expect(hasValidVariables('http://sites.test/${unknown}')).toBeFalsy()
    })
    test('empty variable', () => {
        expect(hasValidVariables('http://sites.test/${}')).toBeFalsy()
    })
    test('last unknown variable', () => {
        expect(hasValidVariables('http://sites.test/${course.id}/${unknown}')).toBeFalsy()
    })
    test('badly formatted', () => {
        expect(hasValidVariables('http://sites.test/${unknown')).toBeTruthy()
    })
})
